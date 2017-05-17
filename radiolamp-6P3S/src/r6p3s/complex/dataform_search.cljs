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
            [r6p3s.ui.font-icon :as font-icon]
            [r6p3s.ui.button :as button]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.toggle-button :as toggle-button]
            [r6p3s.cpt.toggle-buttons-selector :as toggle-buttons-selector]
            [r6p3s.cpt.nav-tabs :as nav-tabs]
            [r6p3s.cpt.select :as select]
            [r6p3s.cpt.textarea :as textarea]
            [r6p3s.cpt.multi-select :as multi-select]))


(def ^:const padding 4)

(declare selector-app-init
         selector-fill-rbs
         selector
         selector-selected
         selector-selected-set
         selector-selected-clean
         selector-selected-as-panel)


(defn make-app-init
  "Инициализация данных"
  [rbs-scheme]
  {:on        toggle-button/app-init
   :rbtype    (->> rbs-scheme
                   :realtype
                   seq
                   (sort-by (comp :ord second))
                   (map-indexed (fn [i [k r]]
                                  [i (assoc r :realtype k)]))
                   (nav-tabs/app-state-i-maker)
                   (assoc nav-tabs/app-state :tabs))
   :selectors (->> rbs-scheme
                   :fields seq
                   (reduce
                    (fn [a [k v]]
                      (assoc a k (selector-app-init k v)))
                    {}))})

(defn selected-clean
  "Отчистка выбранных значений"
  [app rbs-scheme]
  (let [fields (rbs-scheme :fields)]
    (update-in
     app [:selectors]
     (fn [selectors]
       (->> selectors
            keys
            (reduce
             (fn [selectors field]
               (update-in selectors [field] selector-selected-clean (fields field)))
             selectors))))))



(defn selected
  "Формирование выбранных значений"
  [{:keys [on rbtype selectors] :as app}
   {:keys [fields] :as rbs-scheme}]
  (let [on?           (toggle-button/value on)
        realtype      (-> rbtype nav-tabs/active-tab-row :realtype)
        common-fields (get-in rbs-scheme [:common :fields] #{})
        group-fields  (get-in rbs-scheme [:realtype realtype :fields] #{})]
    {:on?       on?
     :realtype  (when on? realtype)
     :selectors (when on?
                  (->> selectors seq
                       (filter (fn [[k _]]
                                 (or (common-fields k)
                                     (group-fields k))))
                       (map (fn [[field app]]
                              {:field    field
                               :selected (selector-selected app (fields field))}))))}))




(defn selected-set
  "Восстановление состояния из selected"
  [app
   {:keys [on realtype selectors]}
   {:keys [fields] :as rbs-scheme}]

  (-> app
      (selected-clean rbs-scheme)
      (update-in [:on] toggle-button/set-value! on)
      (update-in [:rbtype] nav-tabs/set-active-tab-by :realtype realtype)
      (update-in [:selectors]
                 (fn [app]
                   (reduce
                    (fn [app {:keys [field selected]}]
                      (update-in app [field] selector-selected-set (fields field) selected))
                    app selectors)))))



(defn selected-render
  "Панель отображения выбранных элементов"
  [{:keys [on? realtype selectors]}
   {:keys [fields] :as rbs-scheme}
   rbs-data
   opts]
  (let [{:keys [icon text]} (get-in rbs-scheme [:realtype realtype])]
    (dom/div nil
             (dom/div nil (dom/b nil "Фильтр: ") (if on? "да" "нет")
                      (when on? (dom/span #js {:className "text-primary"}
                                       " - " (font-icon/render icon) " " text)))
             (->> selectors
                  (map (fn [{:keys [field selected]}]
                         (let [{:keys [rbentity rbtype] :as meta} (fields field)]
                           (selector-selected-as-panel
                            selected meta (get-in rbs-data [rbentity rbtype] {}) opts))))
                  (filter (comp not nil?))
                  (interpose ", ")
                  (apply dom/div nil)))))





(defn component
  "Визуальный компонент для формирвоания окна поиска по форме"
  [app own {:keys [uri-rbs rbs-scheme chan-update cell-opts show-mast-go-on?]}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update    (or chan-update (chan))
       :chan-update-rb (chan)})

    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update chan-update-rb]} (om/get-state own)]

        (when show-mast-go-on?
          (om/transact! app :on #(toggle-button/set-value! % true)))

        (go
          (while true
            (let [_ (<! chan-update-rb)]
              (rnet/get-data
               uri-rbs
               {}
               (fn [rbs-list]
                 (om/transact!
                  app :selectors
                  (fn [app]
                    (->> rbs-scheme
                         :fields seq
                         (reduce
                          (fn [app [k {:keys [rbentity rbtype] :as m}]]
                            (update-in app [k] selector-fill-rbs m (get-in rbs-list [rbentity rbtype] (list))))
                          app)))))))))

        (put! chan-update-rb 1)))


    om/IRenderState
    (render-state [_ {:keys [chan-update chan-update-rb]}]
      (let [{:keys [on rbtype]}     @app
            on?                     (or show-mast-go-on? (toggle-button/value on))
            common-fields           (get-in rbs-scheme [:common :fields] #{})
            {:keys [fields] :as rb} (nav-tabs/active-tab-row rbtype)]

        (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                      :style     #js {:padding padding}}

                 (dom/div #js {:style #js {:backgroundColor "#eee"
                                           :padding         padding
                                           :borderRadius    (if on? "5px 5px 0px 0px" "5px")}}

                          (if show-mast-go-on?
                            (glyphicon/render "filter")
                            (om/build toggle-button/component (app :on)
                                      {:opts {:text-on  (glyphicon/render "filter")
                                              :text-off (glyphicon/render "filter")
                                              :onClick-fn
                                              (fn [_]
                                                (put! chan-update 1))}}))

                          (if on?
                            (dom/span #js {:className "text-warning"} " Фильтрация по параметрам")
                            (dom/span #js {:className "text-muted"}  " Фильтр отключен"))


                          (when on?
                            (button/render {:text  (glyphicon/render "unchecked")
                                            :style #js {:float "right"}
                                            :type  :warning
                                            :on-click
                                            (fn []
                                              (om/transact! app #(selected-clean % rbs-scheme))
                                              (put! chan-update-rb 1)
                                              (put! chan-update 1))})))

                 (dom/div #js {:style #js {:backgroundColor "#eee"
                                           :padding         padding
                                           :borderRadius    "0px 0px 5px 5px"
                                           :display         (if on? "" "none")}}

                          (om/build nav-tabs/component (app :rbtype)
                                    {:opts {:on-select-fn #(put! chan-update 1)}})

                          (dom/hr #js {:style #js {:marginTop    4
                                                   :marginBottom 2
                                                   :border       1
                                                   :borderStyle  "solid"}})


                          (->> rbs-scheme
                               :fields seq
                               (filter (fn [[k _]]
                                         (or (common-fields k)
                                             (fields k))))
                               (sort-by (comp :ord second))
                               (map (fn [[k m]]
                                      (selector (get-in app [:selectors k]) m chan-update cell-opts)))
                               (apply dom/div #js {:className "row"
                                                   :style     #js {:marginRight 0
                                                                   :marginLeft  0}}))))))))




(defn- cell
  [{:keys [class label-class input-class]
    :or   {class       "col-xs-12 col-sm-6 col-md-4 col-lg-4"
           label-class "col-xs-12 col-sm-5 col-md-5 col-lg-5"
           input-class "col-xs-12 col-sm-7 col-md-7 col-lg-7"}}
   t e]
  (dom/div #js {:className class
                :style     #js {:padding 4 :marginLeft 0 :marginRight 0}}
           (dom/div #js {:className label-class
                         :style     #js {:textAlign "right"
                                         :padding   4}}
                    t)
           (dom/div #js {:className input-class
                         :style     #js {:textAlign "left"
                                         :padding   4}}
                    e)))


(defn- cell-panel
  [opts class-name t e]
  (dom/span #js {:className class-name :style #js {:whiteSpace "nowrap"}}
            (glyphicon/render "asterisk")
            (dom/b nil t ": ")
            (dom/span #js {:className "text-success"} e)))



;;;**************************************************************************************************
;;;* BEGIN DataForm search input components
;;;* tag: <dataform search input components>
;;;*
;;;* description: Компоненты выбора для датаформы поиска
;;;*
;;;**************************************************************************************************


(defmulti selector-app-init          (fn [_ {{type :type} :search}] type))
(defmulti selector-fill-rbs          (fn [_ {{type :type} :search} _] type))
(defmulti selector                   (fn [_ {{type :type} :search} _ opts] type))
(defmulti selector-selected          (fn [_ {{type :type} :search}] type))
(defmulti selector-selected-set      (fn [_ {{type :type} :search} _] type))
(defmulti selector-selected-clean    (fn [_ {{type :type} :search}] type))
(defmulti selector-selected-as-panel (fn [_ {{type :type} :search} _ opts] type))


;;------------------------------------------------------------------------------
;; BEGIN: default
;; tag: <default dataform search component>
;; description: Компонент и поведение по умолчанию
;;------------------------------------------------------------------------------


;; Если селектор не реализован
(defmethod selector-app-init :default
  [_ _]
  {})

(defmethod selector-fill-rbs :default
  [app _ _]
  app)

(defmethod selector :default
  [_ {:keys [text] :as v} _ opts]
  (cell opts text (dom/div #js {:className "text-danger"} "компонент не определен")))

(defmethod selector-selected :default
  [_ row]
  nil)

(defmethod selector-selected-set :default
  [app meta selected]
  (println "meta: " meta)
  (println "app: " app)
  (println "selected: " selected)
  (println)
  app)

(defmethod selector-selected-clean :default
  [app meta]
  (println "meta: " meta)
  (println "app: " app)
  (println)
  (selector-app-init app meta))

(defmethod selector-selected-as-panel :default
  [selected {{type :type} :search :as meta} _ opts]
  (println "type: " type)
  (println "selected: " selected)
  (println)
  (cell-panel opts "text-danger" (str type) (str selected)))

;; END default
;;..............................................................................


;;------------------------------------------------------------------------------
;; BEGIN: multi-buttons
;; tag: <multi-buttons dataform search component>
;; description: Выбор кнопками нескольких возможных значений
;;------------------------------------------------------------------------------


(defmethod selector-app-init :multi-buttons
  [_ {{:keys [buttons]} :search}]
  (mapv
   (fn [row]
     (merge toggle-button/app-init row))
   buttons))

(defmethod selector :multi-buttons
  [app {:keys [text] {:keys [buttons]} :search} chan-update opts]
  (->> buttons
       (map-indexed
        (fn [i {:keys [text]}]
          (om/build toggle-button/component (get-in app [i])
                    {:opts {:text-on text :text-off text
                            :onClick-fn
                            (fn []
                              (put! chan-update 1))}})))
       (apply dom/div #js {:className "btn-group"})
       (cell opts text)))

(defmethod selector-selected :multi-buttons
  [app _]
  (->> app
       (filter toggle-button/value)
       (map #(select-keys % [:cmp :val]))))

(defmethod selector-selected-set :multi-buttons
  [app _ selected]
  (let [selected-vals (->> selected (map :val) set)]
    (mapv
     (fn [{:keys [val] :as row}]
       (toggle-button/set-value! row (contains? selected-vals val)))
     app)))

(defmethod selector-selected-as-panel :multi-buttons
  [selected {:keys [text]} _ opts]
  (when-not (empty? selected)
    (->> selected
         (map (fn [{:keys [val cmp]}]
                (str val (if (= cmp :<=) "+" ""))))
         (clojure.string/join ",")
         (cell-panel opts "" text))))


;; END multi-buttons
;;..............................................................................


;;------------------------------------------------------------------------------
;; BEGIN: band-integer-from
;; tag: <band-integer-from dataform search component>
;; description: Диапазон выбора от
;;------------------------------------------------------------------------------


(defmethod selector-app-init :band-integer-from
  [_ _]
  input/app-init)


(defmethod selector :band-integer-from
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (dom/div #js {:className ""}
                 (om/build input/component app
                           {:opts {:style       #js {:width "47%" :float "left"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "от"
                                   :onChange-updated-fn
                                   (fn []
                                     (put! chan-update 1))}}))))


(defmethod selector-selected :band-integer-from
  [app _]
  (-> app input/value rc/parse-int-or-nil))


(defmethod selector-selected-set :band-integer-from
  [app _ selected]
  (input/set-value! app selected))

(defmethod selector-selected-as-panel :band-integer-from
  [selected {:keys [text]} _ opts]
  (when-not (nil? selected)
    (cell-panel opts "" text (str "oт " selected))))


;; END band-integer-from
;;..............................................................................


;;------------------------------------------------------------------------------
;; BEGIN: band-integer-to
;; tag: <band-integer-to dataform search component>
;; description: Диапазон выбора целых до
;;------------------------------------------------------------------------------

(defmethod selector-app-init :band-integer-to
  [_ _]
  input/app-init)


(defmethod selector :band-integer-to
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (dom/div #js {:className ""}
                 (om/build input/component app
                           {:opts {:style       #js {:width "47%" :float "left"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "до"
                                   :onChange-updated-fn
                                   (fn []
                                     (put! chan-update 1))}}))))


(defmethod selector-selected :band-integer-to
  [app _]
  (-> app input/value rc/parse-int-or-nil))


(defmethod selector-selected-set :band-integer-to
  [app _ selected]
  (input/set-value! app selected))

(defmethod selector-selected-as-panel :band-integer-to
  [selected {:keys [text]} _ opts]
  (when-not (nil? selected)
    (cell-panel opts "" text (str "до " selected))))


;; END band-integer-to
;;..............................................................................


;;------------------------------------------------------------------------------
;; BEGIN: band-integer-from-to
;; tag: <band-integer-from-to dataform search component>
;; description: компонент диапазона выбора для целых от и до
;;------------------------------------------------------------------------------

(defmethod selector-app-init :band-integer-from-to
  [_ {{:keys [buttons]} :search}]
  {:from input/app-init
   :to   input/app-init})


(defmethod selector :band-integer-from-to
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (dom/div #js {:className ""}
                 (om/build input/component (app :from)
                           {:opts {:style       #js {:width "47%" :float "left"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "от"
                                   :onChange-updated-fn
                                   (fn []
                                     (put! chan-update 1))}})
                 (om/build input/component (app :to)
                           {:opts {:style       #js {:width "47%" :float "right"}
                                   :type        "number"
                                   :min         0
                                   :placeholder "до"
                                   :onChange-updated-fn
                                   (fn []
                                     (put! chan-update 1))}}))))


(defmethod selector-selected :band-integer-from-to
  [{:keys [from to]} _]
  {:from (-> from input/value rc/parse-int-or-nil)
   :to   (-> to   input/value rc/parse-int-or-nil)})


(defmethod selector-selected-set :band-integer-from-to
  [app _ {:keys [from to]}]
  (-> app
      (update-in [:from] input/set-value! from)
      (update-in [:to] input/set-value! to)))


(defmethod selector-selected-as-panel :band-integer-from-to
  [{:keys [from to]} {:keys [text]} _ opts]
  (when (or (not (nil? from)) (not (nil? to)))
    (cell-panel opts "" text (str "от " (or from "...") " до " (or to "...")))))


;; END band-integer-from-to
;;..............................................................................

;;------------------------------------------------------------------------------
;; BEGIN: boolean
;; tag: <boolean dataform search component>
;; description: Выбор да или нет
;;------------------------------------------------------------------------------

(defmethod selector-app-init :boolean
  [_ _]
  toggle-button/app-init)


(defmethod selector :boolean
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (om/build toggle-button/component app
                  {:opts {:onClick-fn
                          (fn []
                            (put! chan-update 1))}})))


(defmethod selector-selected :boolean
  [app _]
  (toggle-button/value app))


(defmethod selector-selected-set :boolean
  [app _ selected]
  (toggle-button/set-value! app selected))


(defmethod selector-selected-as-panel :boolean
  [selected {:keys [text]} _ opts]
  (when-not (nil? selected)
    (cell-panel opts "" text (if selected "да" "нет"))))


;; END boolean
;;..............................................................................


;;------------------------------------------------------------------------------
;; BEGIN: boolean-nm
;; tag: <boolean-nm dataform search component>
;; description: Выбор да нет неважно
;;------------------------------------------------------------------------------

(defmethod selector-app-init :boolean-nm
  [k _]
  (toggle-buttons-selector/app-init
   [{:key   :nm
     :text  "неважно"
     :value true}
    {:key  :y
     :text "да"}
    {:key  :n
     :text "нет"}]))


(defmethod selector :boolean-nm
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (om/build toggle-buttons-selector/component app
                  {:opts {:selection-type :one
                          :onClick-fn
                          (fn [_]
                            (put! chan-update 1))}})))


(defmethod selector-selected :boolean-nm
  [app _]
  (toggle-buttons-selector/get-selected-one app))


(defmethod selector-selected-set :boolean-nm
  [app _ selected]
  (toggle-buttons-selector/set-selected app [selected]))

(defmethod selector-selected-as-panel :boolean-nm
  [selected {:keys [text]} _ opts]
  (condp = selected
    :y (cell-panel opts "" text "да")
    :n (cell-panel opts "" text "нет")
    nil))

;; END boolean-nm
;;..............................................................................

;;------------------------------------------------------------------------------
;; BEGIN: rbs-multi-select
;; tag: <rbs-multi-select dataform search component>
;; description: компонент множественного выбора из справочника
;;------------------------------------------------------------------------------

(defmethod selector-app-init :rbs-multi-select
  [_ _]
  multi-select/app-init)

(defmethod selector-fill-rbs :rbs-multi-select
  [app _ data]
  (multi-select/data-set app (vec data)))


(defmethod selector :rbs-multi-select
  [app {:keys [text]} chan-update opts]
  (cell opts text
        (om/build multi-select/component app
                  {:opts {:on-select-fn
                          (fn [_]
                            (put! chan-update 1))}})))


(defmethod selector-selected :rbs-multi-select
  [app _]
  (->> app
       multi-select/selected
       (map :id)))


(defmethod selector-selected-set :rbs-multi-select
  [app _ selected]
  (multi-select/selected-set-for app :id selected))


(defmethod selector-selected-clean :rbs-multi-select
  [app _]
  (multi-select/selected-clean app))


(defmethod selector-selected-as-panel :rbs-multi-select
  [selected {:keys [text]} rbs-data opts]
  (when-not (empty? selected)
    (let [selected (set selected)]
      (cell-panel opts "" text
                  (str "["
                       (->> rbs-data
                            (filter (comp selected :id))
                            (map :keyname)
                            (clojure.string/join ","))
                       "]")))))

;; END rbs-multi-select
;;..............................................................................



;;; END DataForm search input components
;;;..................................................................................................
