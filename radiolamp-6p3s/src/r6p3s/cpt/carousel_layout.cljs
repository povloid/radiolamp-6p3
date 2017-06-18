(ns r6p3s.cpt.carousel-layout
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]))


(def app-init
  {:active 0})


(defn component [app own {:keys [pages content-vec] :as opts
                          :or   {pages       1
                                 content-vec [(dom/h1 nil "1")
                                              (dom/h2 nil "2")]}}]
  (reify

    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div
       #js {:className ""
            :style     #js {:padding 0}}

       (->> content-vec
            (map-indexed (fn [i element]
                           (dom/div #js {:style #js {:display
                                                     (if (= (:active @app) i)
                                                       "" "none") }}
                                    element)))
            (apply dom/div #js {:className "col-xs-11 col-sm-11 col-md-11 col-lg-11"
                                :style     #js {:padding 0}}))

       (dom/div #js {:className "col-xs-1 col-sm-1 col-md-1 col-lg-1"
                     :style     #js {:paddingLeft 4}}
                (button/render {:text     (glyphicon/render "chevron-up")
                                :on-click (fn []
                                            (om/transact!
                                             app :active
                                             #(let [i (dec %)]
                                                (if (< i 0) (dec pages) i))))})
                (dom/div #js {})
                (button/render {:text     (glyphicon/render "chevron-down")
                                :style     #js {:marginTop 4}
                                :on-click (fn []
                                            (om/transact!
                                             app :active
                                             #(let [i (inc %)]
                                                (if (<= pages i) 0 i))))}))))))
