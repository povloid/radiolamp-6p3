(ns r6p3s.complex.dataform-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.common-input :as common-input]
            [r6p3s.core :as rc]
            [r6p3s.net :as rnet]
            [r6p3s.ui.panel-with-table :as panel-with-table]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.toggle-button :as toggle-button]
            [r6p3s.cpt.toggle-buttons-selector :as toggle-buttons-selector]
            [r6p3s.cpt.nav-tabs :as nav-tabs]
            [r6p3s.cpt.select :as select]
            [r6p3s.cpt.textarea :as textarea]
            [r6p3s.cpt.toggle-button :as toggle-button]))

(def ^:const padding 4)

(declare selector-app-init selector-fill-rbs selector)


(defn make-app-init
  "Инициализация данных"
  [rbs-scheme]
  {:on        toggle-button/app-init
   :rbtype    (->> rbs-scheme
                   :realtype
                   vals
                   (sort-by :ord)
                   (map-indexed vector)
                   (nav-tabs/app-state-i-maker)
                   (assoc nav-tabs/app-state :tabs))
   :selectors (->> rbs-scheme
                   :fields seq
                   (reduce
                    (fn [a [k v]]
                      (assoc a k (selector-app-init k v)))
                    {}))})




(defn component
  "Визуальный компонент для формирвоания окна поиска по форме"
  [app own {:keys [uri-rbs rbs-scheme]}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})

    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update]} (om/get-state own)]
        (go 
          (while true
            (let [_ (<! chan-update)]
              (rnet/get-data
               uri-rbs
               {}
               (fn [rbs-list]
                 (println rbs-list)                
                 (om/transact!
                  app :selectors
                  (fn [app]
                    (->> rbs-scheme
                         :fields seq
                         (reduce
                          (fn [app [k {:keys [rbentity rbtype] :as m}]]
                            (update-in app [k] selector-fill-rbs m (get-in rbs-list [rbentity rbtype] (list))))
                          app)))))))))
        
        (put! chan-update 1)))

    
    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (let [{:keys [on rbtype]}     @app
            on?                     (toggle-button/value on)
            common-fields           (get-in rbs-scheme [:common :fields] #{})
            {:keys [fields] :as rb} (nav-tabs/active-tab-row rbtype)]
        (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                      :style     #js {:padding padding}}

                 (dom/div #js {:style #js {:backgroundColor "#eee"
                                           :padding         padding
                                           :borderRadius    (if on? "5px 5px 0px 0px" "5px")}}
                          (om/build toggle-button/component (app :on)
                                    {:opts {:text-on  (glyphicon/render "filter")
                                            :text-off (glyphicon/render "filter")}})

                          (if on?
                            (dom/span #js {:className "text-warning"} " Фильтрация по параметрам")
                            (dom/span #js {:className "text-muted"}  " Фильтр отключен")))

                 (dom/div #js {:style #js {:backgroundColor "#eee"
                                           :padding         padding
                                           :borderRadius    "0px 0px 5px 5px"
                                           :display         (if on? "" "none")}}

                          (om/build nav-tabs/component (app :rbtype))

                          ;;(str fields)

                          (->> rbs-scheme
                               :fields seq
                               (filter (fn [[k _]]
                                         (or (common-fields k)
                                             (fields k))))
                               (map (fn [[k m]]
                                      (selector (get-in app [:selectors k]) m)))
                               (apply dom/div #js {:className "row"
                                                   :style     #js {:marginRight 0
                                                                   :marginLeft  0}}))))))))




(defn- cell [t e]
  (dom/div #js {:className "col-xs-12 col-sm-6 col-md-4 col-lg-4"
                :style     #js {:padding 4 :marginLeft 0 :marginRight 0}}
           (dom/div #js {:className "col-xs-12 col-sm-5 col-md-5 col-lg-5"
                         :style     #js {:textAlign "right"
                                         :padding   4}}
                    t)
           (dom/div #js {:className "col-xs-12 col-sm-7 col-md-7 col-lg-7"
                         :style     #js {:textAlign "left"
                                         :padding   4}}
                    e)))








(defmulti selector-app-init (fn [_ {{stype :type} :search}] stype))
(defmulti selector-fill-rbs (fn [_ {{stype :type} :search} _] stype))
(defmulti selector          (fn [_ {{stype :type} :search}] stype))



;; Если селектор не реализован
(defmethod selector-app-init :default
  [_ _]
  {})
(defmethod selector-fill-rbs :default
  [app _ _]
  app)
(defmethod selector :default
  [_ {:keys [text search] :as v}]
  (cell text (str search)))





(defmethod selector-app-init :multi-buttons
  [k {{:keys [buttons]} :search}]
  (mapv
   (fn [row]
     (merge toggle-button/app-init row))
   buttons))

(defmethod selector :multi-buttons
  [app {:keys [text] {:keys [buttons]} :search}]
  (->> buttons
       (map-indexed
        (fn [i {:keys [text]}]
          (om/build toggle-button/component (get-in app [i])
                    {:opts {:text-on text :text-off text}})))
       (apply dom/div #js {:className "btn-group"})
       (cell text)))





(defmethod selector-app-init :band-integer
  [k {{:keys [buttons]} :search}]
  {:from input/app-init
   :to   input/app-init})

(defmethod selector :band-integer
  [app {:keys [text]}]
  (cell text
        (dom/div #js {:className ""}
                 (om/build input/component (app :from)
                           {:opts {:style       #js {:width "47%" :float "left"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "от"}})
                 (om/build input/component (app :to)
                           {:opts {:style       #js {:width "47%" :float "right"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "до"}}))))







(defmethod selector-app-init :boolean
  [k {{:keys [buttons]} :search}]
  toggle-button/app-init)

(defmethod selector :boolean
  [app {:keys [text]}]
  (cell text
        (om/build toggle-button/component app)))





(defmethod selector-app-init :boolean-nm
  [k {{:keys [buttons]} :search}]
  (toggle-buttons-selector/app-init
   [{:key  :nm
     :text "неважно"
     :value true}
    {:key  :y
     :text "да"}
    {:key  :n
     :text "нет"}]))

(defmethod selector :boolean-nm 
  [app {:keys [text]}]
  (cell text
        (om/build toggle-buttons-selector/component app
                  {:opts {:selection-type :one}})))





(defmethod selector-app-init :rbs-multi-select
  [k {{:keys [buttons]} :search}]
  {:data []})

(defmethod selector-fill-rbs :rbs-multi-select
  [app m data]
  (assoc app :data (vec data)))

(defmethod selector :rbs-multi-select
  [app {:keys [text]}]
  (cell text
        (dom/div nil (str @app))))
