(ns ix.omut.component.thumbinal
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [clojure.set :as clojset]
            [clojure.string :as clojstr]))




(def app-init
  {:id              nil
   :path            nil
   :top_description nil
   :description     nil
   :galleria        false
   })


(defn component [app _ {:keys [class+ onClick-fn]
                        :or   {class+ "col-xs-12 col-sm-6 col-md-4 col-lg-4"}}]
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
               :style      #js {:cursor "pointer"}}
          (when galleria
            (dom/span #js {:className   "glyphicon glyphicon-film"
                           :style       #js {:position "absolute"
                                             :top      10 :left 5
                                             :fontSize "2em"}
                           :aria-hidden "true"}))
          (dom/a nil (dom/img #js {:src (str path "_as_300.png") :alt "фото"
                                   ;;:style #js {:width "100%"}
                                   }))
          (dom/div #js {:className "caption"}
                   (when (not (clojstr/blank? top_description))
                     (dom/h4 nil top_description))
                   (dom/div nil
                            (when (not (clojstr/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"
                                           :style     #js {:width "15%" :fontSize "0.7em"}} "URL")
                            " "
                            (dom/input #js {:type        "text"
                                            :style       #js {:width "65%" :fontSize "0.7em"}
                                            :value       path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))
