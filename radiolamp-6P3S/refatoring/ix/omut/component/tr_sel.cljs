(ns ix.omut.component.tr-sel
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]))

(defn component [app owner {:keys [app-to-tds-seq-fn
                                   clear-selections-fn
                                   on-select-fn
                                   class-fn]
                            :or   {app-to-tds-seq-fn
                                   (fn [row]
                                     (map
                                      #(dom/td nil %)
                                      (-> row
                                          (select-keys [:id :keyname :description])
                                          vals)))}}]
  (letfn [(on-click [app e]
            (.preventDefault e)
            (.stopPropagation e)

            (when clear-selections-fn
              (clear-selections-fn))

            (c/omut-row-set-selected-not!! app)

            (when on-select-fn
              (on-select-fn @app))

            1)]

    (reify
      om/IWillMount
      (will-mount [_]
        (c/omut-row-if-not-init-init!! app))
      om/IRender
      (render [_]
        (apply dom/tr #js {:className (str
                                       (if class-fn (class-fn @app) "") " "
                                       (if (c/omut-row-selected? @app) "info" ""))
                           :onClick   (partial on-click app)
                           ;;:onTouchEnd (partial on-click app) ;; недает проматывать
                           }
               (app-to-tds-seq-fn app) )))))
