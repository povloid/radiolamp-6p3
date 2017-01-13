(ns r6p3s.cpt.input-search-selector
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.common-input :as common-input]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.tbody-trs-sel :as tbody-trs-sel]
            [r6p3s.cpt.helper-p :as helper-p]))






;; -------------------------------------------------------------------------------------------
;; TODO вынести данную функцию в общий функционал
(defn select-step [ks data]
  (let [data (vec data)]
    (vec
     (cond (empty? data)              data
           (some #(get-in % ks) data) (->> (conj (vec (rest data)) (first data))
                                           (map
                                            (fn [r1 r2]
                                              (assoc-in r1 ks (get-in r2 ks)))
                                            data))
           :else                      (update-in data [(dec (count data))]
                                                 assoc-in ks true)))))

;; TODO вынести данную функцию в общий функционал
(defn select-step-next [ks data]
  (->> data
       reverse
       (select-step ks)
       reverse
       vec))
;; -------------------------------------------------------------------------------------------




(def app-init
  {:selected nil
   :input    input/app-init
   :data     []})



(defn get-selected [app]
  (:selected app))


(defn set-selected
  ([app row]
   (set-selected app row {:text-fn :keyname}))
  ([app row {:keys [text-fn]}]
   (-> app
       (assoc :selected row)
       (update-in [:input] input/set-value! (text-fn row)))))


(def select-step-omut (partial select-step [:omut-row :selected]))
(def select-step-next-omut (partial select-step-next [:omut-row :selected]))





(defn component
  [app own
   {:keys [class+
           search-data-fn
           text-fn
           ui-row-fn]
    :or   {}
    :as   opts}]

  (let [text-fn        :keyname
        search-data-fn (or search-data-fn
                           (fn [chan-ret-data text]
                             (println '>>> text)
                             (->> {}
                                  (repeat 10)
                                  (map-indexed
                                   (fn [i row]
                                     (assoc row text-fn (str text " " i))))
                                  vec
                                  (put! chan-ret-data))))
        ui-row-fn      (or ui-row-fn
                           (fn [row]
                             [(dom/td nil (text-fn @row))]))]

    (reify
      om/IInitState
      (init-state [_]
        {:chan-update   (chan)
         :chan-ret-data (chan)
         :in-progress?  false
         :show-popup?   false})

      om/IWillMount
      (will-mount [this]
        (let [{:keys [chan-update
                      chan-ret-data]} (om/get-state own)]

          (go
            (while true
              (let [_ (<! chan-update)]
                )))

          (go
            (while true
              (let [data (<! chan-ret-data)]
                (om/update! app :data data)
                (om/set-state! own :in-progress? false))))

          (put! chan-update 1)))

      om/IRenderState
      (render-state [_ {:keys [chan-update chan-ret-data show-popup?]}]
        (letfn [(search []
                  (om/set-state! own :show-popup? true)
                  (->> @app
                       :input
                       input/value
                       (search-data-fn chan-ret-data)))

                (get-selected []
                  (->> @app
                       :data
                       (filter #(get-in % [:omut-row :selected]))
                       first))

                (open []
                  (om/set-state! own :show-popup? true))

                (close []
                  (om/set-state! own :show-popup? false))

                (clear []
                  (om/set-state! own :show-popup? false)
                  (om/update! app app-init))

                (up-select []
                  (om/set-state! own :show-popup? true)
                  (om/transact! app :data select-step-omut))
                (down-select []
                  (om/set-state! own :show-popup? true)
                  (om/transact! app :data select-step-next-omut))

                (select-value []
                  (if (om/get-state own :show-popup?)
                    (do
                      (om/transact!
                       app (fn [app]
                             (let [selected (get-selected)]
                               (if (empty? selected)
                                 app (set-selected app selected {:text-fn text-fn})))))
                      (om/set-state! own :show-popup? false))
                    ;; Или повторно открываем поиск...
                    (open)))]
          (let [app-v @app]
            (dom/div
             nil
             (dom/div
              #js {:className (str "input-group " class+) :style #js {:marginBottom 6}}

              (dom/span #js {:className "input-group-btn"}
                        (button/render {:type     :default
                                        :on-click clear
                                        :text     (glyphicon/render "remove")}))


              (om/build input/component (app :input)
                        {:opts {:placeholder         "Введите текст для поиска..."
                                :onKeyDown-fn
                                (fn [v]
                                  (let [kod (.-which v)]
                                    (condp = kod
                                      13 (do (select-value)
                                             (.preventDefault v))
                                      38 (do (up-select)
                                             (.preventDefault v))
                                      40 (do (down-select)
                                             (.preventDefault v))
                                      27 (do (close)
                                             (.preventDefault v))
                                      (do (println "key:" kod))))
                                  1)
                                :onChange-updated-fn search}})


              (dom/span #js {:className "input-group-btn"}
                        (button/render {:type     :success
                                        :active?  show-popup?
                                        :on-click open
                                        :text     (glyphicon/render "search")})))

             (let [[icon text-class] (if (= (get-in app-v [:selected text-fn])
                                            (get-in app-v [:input :value]))
                                       ["ok"       "text-success"]
                                       ["asterisk" "text-warning"])]
               (dom/div #js {:className text-class
                             :style     #js {:position "absolute"
                                             :top      8
                                             :right    65
                                             :zIndex   5}}
                        (glyphicon/render icon)))


             (dom/div
              #js {:style #js {:display    (if show-popup? "" "none")
                               :position   "absolute"
                               :width      "100%"
                               :zIndex     1
                               :boxShadow  "0px 2px 8px"
                               :background "white"}}
              (table/render {:hover?      true
                             :bordered?   true
                             :striped?    true
                             :responsive? false
                             :tbody
                             (om/build tbody-trs-sel/component  (:data app)
                                       {:opts {:selection-type    :one
                                               :app-to-tds-seq-fn ui-row-fn
                                               ;;:on-select-fn
                                               ;;(fn [{:keys [] :as row}])
                                               }})
                             })

              (dom/div #js {:className "btn-group"
                            :style     #js {:float  "right"
                                            :margin 4}}

                       (button/render
                        {:type     :success
                         :on-click search
                         :text     (dom/span nil
                                             (glyphicon/render "search")
                                             " Поиск")})

                       (button/render
                        {:type      :primary
                         :on-click  select-value
                         :disabled? (empty? (get-selected))
                         :text      (dom/span nil
                                              (glyphicon/render "ok")
                                              " Выбрать")})
                       (button/render
                        {:type     :default
                         :on-click close
                         :text     (dom/span nil
                                             (glyphicon/render "remove")
                                             " Закрыть")})))

             ;; Отображаем выбраное
             (if (app-v :selected)
               (->> app
                    :selected
                    ui-row-fn
                    (apply dom/tr nil)
                    (dom/tbody nil)
                    (dom/table #js {:style #js {:width "100%"}}))
               (dom/p #js {:className "text-mutted"}
                      "нет.")))))))))











(defn component-form-group
  [app owner {:keys [label
                     type
                     label-class+
                     label-style
                     input-class+
                     spec-input]
              :or   {label        "Метка"
                     label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                     input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                     spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+)
                               :style label-style} label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (om/build helper-p/component app {}))))))
