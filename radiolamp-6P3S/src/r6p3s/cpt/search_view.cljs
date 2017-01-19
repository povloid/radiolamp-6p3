(ns r6p3s.cpt.search-view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            ;;[r6p3s.ui.button :as button]
            [r6p3s.cpt.paginator :as paginator]
            ;;[r6p3s.cpt.input :as input]
            [r6p3s.cpt.input-search :as input-search]))



(def app-init
  (merge {:fts-query input-search/app-init
          :data      []}
         paginator/app-init))




(defn data [app-v]
  (app-v :data))

(defn selected [app-v]
  (->> app-v data
       (filter c/omut-row-selected?)))

(defn selected-first [app-v]
  (first (selected app-v)))



(defn app-data [app]
  (data @app))

(defn app-data-selected [app]
  (selected @app))

(defn app-data-selected-first [app]
  (selected-first @app))


(defn component [app owner
                 {:keys [input-placeholder
                         chan-update
                         data-update-fn
                         data-rendering-fn
                         add-button-fn
                         tools-top
                         tools
                         show-search-input?]
                  :or   {input-placeholder  "введите сюда поисковый запрос"
                         data-update-fn     (fn [app]
                                              (println "Неопределена функция запроса обновления данных (data-update-fn [app] ...)")
                                              (println "параметр на входе: " (str app)))
                         data-rendering-fn  (fn [app]
                                              (println "Неопределена функция запроса перерисовки данных (data-rendering-fn [app] ...)")
                                              (println "параметр на входе: " (str app)))
                         show-search-input? true}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (or chan-update (chan))})
    om/IWillMount
    (will-mount [this]
      (let [chan-update (om/get-state owner :chan-update)]
        (go (loop []
              (let [_ (<! chan-update)]
                ;; data update function
                (data-update-fn app)

                ;; использовалось при разрабтке
                ;; (rnet/get-data "/tc/opt/rest/product/search"
                ;;                   {:fts-query (-> @app :fts-query :value)
                ;;                    :page (@app :page)}
                ;;                   (fn [response]
                ;;                     (om/update! app :data response)))

                (recur))))
        (put! chan-update {})))

    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div
       #js {:className "container-fluid"}
       (dom/div
        #js {:className "row"}

        (when tools-top tools-top)

        
        (when show-search-input?
          ;; новый вариант
          (om/build input-search/component (app :fts-query)
                    {:opts {:class+            "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                            :chan-update       chan-update
                            :clear-fn          #(om/update! app :page 1)
                            :add-fn            add-button-fn
                            :input-placeholder input-placeholder
                            :enter-fn          #(om/update! app :page 1)}})
          ;; старый вариант - теперь вынесен в компонент input-search
                                        ;TODO: в будущем надо стереть
          #_(dom/div #js {:className "input-group col-xs-12 col-sm-12 col-md-12 col-lg-12" :style #js {:marginBottom 6}}
                     (dom/span #js {:className "input-group-btn"}
                               (button/render {:type     :default
                                               :on-click (fn [_]
                                                           (om/update! app :page 1)
                                                           (om/update! app [:fts-query :value] "")
                                                           (put! chan-update 1)
                                                           1)
                                               :text     (dom/span #js {:className   "glyphicon glyphicon-remove"
                                                                        :aria-hidden "true"})
                                               }))
                     (om/build input/component (:fts-query app)
                               {:opts {:placeholder   input-placeholder
                                       :onKeyPress-fn #(do #_(println
                                                              (.-type %)
                                                              (.-which %)
                                                              (.-timeStamp %))

                                                           (when (= 13 (.-which %))
                                                             (do
                                                               (om/update! app :page 1)
                                                               (put! chan-update 1)))
                                                           1)
                                       }})

                     (dom/span #js {:className "input-group-btn"}
                               (button/render {:type     :success
                                               :on-click (fn [_]
                                                           (om/update! app :page 1)
                                                           (put! chan-update 1)
                                                           1)
                                               :text     (dom/span #js {:className   "glyphicon glyphicon-search"
                                                                        :aria-hidden "true"})
                                               })

                               (when add-button-fn
                                 (button/render {:type     :danger
                                                 :on-click (fn [_]
                                                             (add-button-fn)
                                                             1)
                                                 :text     (dom/span #js {:className   "glyphicon glyphicon-plus"
                                                                          :aria-hidden "true"})
                                                 }))

                               )
                     ))

        ;;(dom/br nil)
        (when tools tools)

        ;; top paginator
        (dom/div #js {:className "input-group col-xs-12 col-sm-12"}
                 (om/build paginator/component app {:opts {:chan-update chan-update}}))

        (dom/br nil)

        ;; data rendering component
        (data-rendering-fn app)


        ;; bottom paginator
        (dom/div #js {:className "input-group col-xs-12 col-sm-12"}
                 (om/build
                  paginator/component app
                  {:opts {:chan-update chan-update
                          ;; При отрабатывании прокручивать страницу на верх
                          :on-click-fn #(.scrollTo js/window 0 0)}}))
        (dom/br nil))))))
