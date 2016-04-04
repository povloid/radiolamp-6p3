(ns ix.omut.component.paginator
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [ix.omut.core :as c]))



(def app-init
  {:page      1
   :page-size 10
   :count-all nil})

(defn component [app owner {:keys [chan-update class+ on-click-fn]}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str  "input-group " (if class+ class+ ""))
                    :style     #js {:textAlign "center"
                                    :maxWidth  500
                                    :float     "none"
                                    :margin    "0 auto"}}
               (dom/span #js {:className "input-group-btn"}
                         (c/ui-button {:type     :default
                                       :on-click (fn [_]
                                                   (om/update! app :page 1)
                                                   (when chan-update
                                                     (put! chan-update 1))

                                                   (when on-click-fn (on-click-fn))
                                                   1)
                                       :text     (dom/span #js {:className   "glyphicon glyphicon-fast-backward"
                                                                :aria-hidden "true"})
                                       })

                         (c/ui-button {:type     :default
                                       :on-click (fn [_]
                                                   (om/transact! app :page
                                                                 #(if (= 1 %) % (dec %)))
                                                   (when chan-update
                                                     (put! chan-update 1))

                                                   (when on-click-fn (on-click-fn))
                                                   1)
                                       :text     (dom/span nil
                                                           (dom/span #js {:className   "glyphicon glyphicon-step-backward"
                                                                          :aria-hidden "true"})
                                                           " Назад")
                                       })
                         )


               (let [{:keys [page page-size count-all]} @app]
                 (dom/div
                  #js {:className "input-control"
                       :style #js {:lineHeight 1.2}}
                  " страница "
                  (dom/b nil page)
                  (when (and count-all page-size) " из ")
                  (when (and count-all page-size) (dom/b nil (inc (quot count-all page-size))))

                  (when count-all (dom/br nil))
                  (when count-all "всего записей ")
                  (when count-all (dom/b nil (str count-all)))))

               (dom/span #js {:className "input-group-btn"}
                         (c/ui-button {:type     :default
                                       :on-click (fn [_]
                                                   (om/transact! app :page inc)
                                                   (when chan-update
                                                     (put! chan-update 1))

                                                   (when on-click-fn (on-click-fn))
                                                   1)
                                       :text     (dom/span nil "Вперед "
                                                           (dom/span #js {:className   "glyphicon glyphicon-step-forward"
                                                                          :aria-hidden "true"}))
                                       })

                         )))))
