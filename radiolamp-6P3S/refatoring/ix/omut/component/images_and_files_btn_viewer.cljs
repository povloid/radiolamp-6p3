(ns ix.omut.component.images-and-files-btn-viewer
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]))



(defn component [app own {:keys [chan-thumb-show-in-full-screen]}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [images files]} @app]
        (c/ui-collapser
         app (str "изображений (" (count images) ") файлов (" (count files) ")")
         :k (or (not (empty? images)) (not (empty? files)))
         (apply
          dom/ul #js {:className "list-group"}
          (into
           (reduce
            (fn [a {:keys [id path top_description description] :as row}]
              (conj a (dom/li #js {:className "list-group-item"}
                              (c/ui-media
                               {:media-object         (c/ui-media-object
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
                              (c/ui-media
                               {:media-object (c/ui-glyphicon "file" "" "2em")
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
