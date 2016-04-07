(ns r6p3s.cpt.text-collapser
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]))


(defn component [app _ {k :k}]
  (reify
    om/IWillMount
    (will-mount [_]
      (c/omut-row-if-not-init-init!! app))
    om/IRender
    (render [_]
      (let [text       (app k "")
            text-count (count text)]
        (dom/p nil
               (button/render {:text     "..."
                               :size     :xs
                               :active?  (not (c/omut-row-collapsed? @app k))
                               :on-click #(c/omut-row-set-collapsed-not!! app k)})

               (if (and (c/omut-row-collapsed? @app k) (> text-count 90))
                 (str (.substring text 0 89) "...")
                 text))))))
