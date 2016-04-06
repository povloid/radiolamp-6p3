(ns r6p3s.cpt.file
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]))



;;**************************************************************************************************
;;* BEGIN files
;;* tag: <files>
;;*
;;* description: Компоненты работы с файлами
;;*
;;**************************************************************************************************


(def app-init
  {:id              nil
   :path            nil
   :top_description nil
   :description     nil
   :galleria        false
   })


(defn component [app _ {:keys [class+  onClick-fn]
                        :or   {class+ "col-xs-12 col-sm-12 col-md-12 col-lg-12"}}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id path top_description description galleria]
             :as   row} app
            on-click    (c/on-click-com-fn #(when onClick-fn (onClick-fn row)))]
        (dom/div
         #js {:className class+}
         (dom/div
          #js {:className  "thumbnail"
               :onClick    on-click
               :onTouchEnd on-click
               :style      #js {:cursor    "pointer"
                                :minHeight 75 }}

          (dom/span #js {:className "glyphicon glyphicon-file"
                         :style     #js {:fontSize   "5em"
                                         :float      "left"
                                         :ariaHidden "true"}})

          (dom/div #js {:className "caption"}
                   (when (not (clojure.string/blank? top_description))
                     (dom/h3 nil top_description))
                   (dom/div nil
                            (when (not (clojure.string/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"} "URL")
                            " "
                            (dom/input #js {:type        "text"
                                            :style       #js {:width "70%" :fontSize "0.7em"}
                                            :value       path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))


;; END files
;;..................................................................................................
