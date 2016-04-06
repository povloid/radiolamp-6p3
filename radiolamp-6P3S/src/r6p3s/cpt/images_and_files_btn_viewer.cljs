(ns r6p3s.cpt.images-and-files-btn-viewer
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.collapser :as collapser]
            [r6p3s.ui.media :as media]
            [r6p3s.ui.media-object :as media-object]))



(defn component [app own {:keys [chan-thumb-show-in-full-screen]}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [images files]} @app]
        (collapser/render
         app (str "изображений (" (count images) ") файлов (" (count files) ")")
         :k (or (not (empty? images)) (not (empty? files)))
         (apply
          dom/ul #js {:className "list-group"}
          (into
           (reduce
            (fn [a {:keys [id path top_description description] :as row}]
              (conj a (dom/li #js {:className "list-group-item"}
                              (media/render
                               {:media-object         (media-object/render
                                                       {:src (str path "_as_60.png")})
                                :heading              top_description
                                :on-click-in-image-fn #(put! chan-thumb-show-in-full-screen row)
                                :button-do-text       "cмотреть"
                                :button-do-fn         #(put! chan-thumb-show-in-full-screen row)
                                :body                 description
                                }))))
            [] images)
           (reduce
            (fn [a {:keys [id path filename top_description description] :as row}]
              (conj a (dom/li #js {:className "list-group-item"}
                              (media/render
                               {:media-object (glyphicon/render "file" "" "2em")
                                :href         path
                                :on-click-fn
                                (fn [e]
                                  ;; Далее прервать выполнение события для родительского
                                  ;; компонента
                                  (.stopPropagation e))
                                :heading      (dom/span nil top_description
                                                        " " (dom/small nil filename))
                                :body         description
                                }))))
            [] files))))))))
