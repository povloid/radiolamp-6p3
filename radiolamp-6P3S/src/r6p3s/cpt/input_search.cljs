(ns r6p3s.cpt.input-search
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.cpt.input :as input]))





(def app-init input/app-init)


(defn component [app own {:keys [input-placeholder chan-update clear-fn enter-fn add-fn class+]
                          :or {input-placeholder  "введите сюда поисковый запрос"}}]
  (reify
    ;; om/IInitState
    ;; (init-state [_]
    ;;   {:chan-update (chan)})

    ;; om/IWillMount
    ;; (will-mount [this]
    ;;   (let [{:keys [chan-update]} (om/get-state own)]
    ;;     (println "OM: componenet -> will-mount")

    ;;     (go
    ;;       (while true
    ;;         (let [_ (<! chan-update)]
    ;;           (println "OM: componenet -> chan-update -> run! "))))

    ;;     (put! chan-update 1)))

    ;; om/IDidMount
    ;; (did-mount [_]
    ;;   (let [{:keys []} (om/get-state own)]
    ;;     (println "OM: componenet -> did-mount")))

    ;; om/IWillUnmount
    ;; (will-unmount [_]
    ;;   (let [{:keys []} (om/get-state own)]
    ;;     (println "OM: componenet -> will-unmount")))


    om/IRenderState
    (render-state [_ _]
      (dom/div #js {:className (str "input-group " class+) :style #js {:marginBottom 6}}
               (dom/span #js {:className "input-group-btn"}
                         (button/render {:type     :default
                                         :on-click (fn [_]
                                                     (om/update! app :value "")
                                                     (when clear-fn (clear-fn))
                                                     (put! chan-update 1)
                                                     1)
                                         :text     (dom/span #js {:className   "glyphicon glyphicon-remove"
                                                                  :aria-hidden "true"})
                                         }))
               (om/build input/component app
                         {:opts {:placeholder   input-placeholder
                                 :onKeyPress-fn #(do #_(println
                                                        (.-type %)
                                                        (.-which %)
                                                        (.-timeStamp %))

                                                     (when (= 13 (.-which %))
                                                       (do
                                                         (when enter-fn (enter-fn))
                                                         (put! chan-update 1)))
                                                     1)
                                 }})

               (dom/span #js {:className "input-group-btn"}
                         (button/render {:type     :success
                                         :on-click (fn [_]
                                                     (when enter-fn (enter-fn))
                                                     (put! chan-update 1)
                                                     1)
                                         :text     (dom/span #js {:className   "glyphicon glyphicon-search"
                                                                  :aria-hidden "true"})
                                         })

                         (when add-fn
                           (button/render {:type     :danger
                                           :on-click (fn [_]
                                                       (add-fn)
                                                       1)
                                           :text     (dom/span #js {:className   "glyphicon glyphicon-plus"
                                                                    :aria-hidden "true"})})))))))
