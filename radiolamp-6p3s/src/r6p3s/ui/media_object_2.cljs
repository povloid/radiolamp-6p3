(ns r6p3s.ui.media-object-2
  (:require [r6p3s.ui.media-object :as media-object]
            [r6p3s.ui.glyphicon :as glyphicon]))

(defn render [avatar px & [or-element]]
  (if (empty? avatar)
    (or
     or-element
     (glyphicon/render "tree-conifer" nil (str px "px")))
    (media-object/render
     {:class+ ""
      :src (str avatar "_as_" px ".png")
      :style #js {:width px}})))
