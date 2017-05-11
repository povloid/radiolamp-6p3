(ns r6p3s.cpt.paginator
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]))



(def app-init
  {:page      1
   :page-size 10
   :count-all nil})

(defn page [app]
  (:page app))

(defn set-page [app page]
  (assoc app :page page))

(defn set-page-1 [app]
  (set-page app 1))

(defn component [app owner {:keys [chan-update class+ on-click-fn]}]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [page page-size count-all]} @app
            and-count-all-page-size?           (and count-all page-size)]
        (dom/div #js {:className (str  "input-group " (if class+ class+ ""))
                      :style     #js {:textAlign "center"
                                      :maxWidth  500
                                      :float     "none"
                                      :margin    "0 auto"}}
                 (dom/span #js {:className "input-group-btn"}
                           (button/render {:type     :default
                                           :on-click (fn [_]
                                                       (om/update! app :page 1)
                                                       (when chan-update
                                                         (put! chan-update 1))

                                                       (when on-click-fn (on-click-fn))
                                                       1)
                                           :text (glyphicon/render "fast-backward")
                                           })

                           (button/render {:type     :default
                                           :on-click (fn [_]
                                                       (om/transact! app :page
                                                                     #(if (= 1 %) % (dec %)))
                                                       (when chan-update
                                                         (put! chan-update 1))

                                                       (when on-click-fn (on-click-fn))
                                                       1)
                                           :text (dom/span nil
                                                           (glyphicon/render "step-backward")
                                                           " Назад")
                                           })
                           )


                 (dom/div
                  #js {:className "input-control"
                       :style     #js {:lineHeight 1.2}}
                  " страница "
                  (dom/b nil page)
                  (when and-count-all-page-size? " из ")
                  (when and-count-all-page-size? (dom/b nil (+ (quot count-all page-size)
                                                               (if (< 0 (rem count-all page-size)) 1 0))))

                  (when count-all (dom/br nil))
                  (when count-all "всего записей ")
                  (when count-all (dom/b nil (str count-all))))

                 (dom/span #js {:className "input-group-btn"}
                           (button/render {:type     :default
                                           :on-click (fn [_]
                                                       (om/transact! app :page inc)
                                                       (when chan-update
                                                         (put! chan-update 1))

                                                       (when on-click-fn (on-click-fn))
                                                       1)
                                           :text (dom/span nil "Вперед "
                                                           (glyphicon/render "step-forward"))
                                           })

                           (when and-count-all-page-size?
                             (button/render {:type     :default
                                             :on-click (fn [_]
                                                         (om/update! app :page (inc (quot count-all page-size)))
                                                         (when chan-update
                                                           (put! chan-update 1))

                                                         (when on-click-fn (on-click-fn))
                                                         1)
                                             :text (dom/span nil
                                                             (glyphicon/render "fast-forward"))
                                             }))))))))
