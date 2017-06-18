(ns r6p3s.cpt.one-image-uploder
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.cpt.file-uploder :as file-uploder]
            [r6p3s.core :as c]            
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.media-object :as media-object]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]            
            [r6p3s.cpt.helper-p :as helper-p]))



;;**************************************************************************************************
;;* BEGIN Upload one image
;;* tag: <one image uploader>
;;*
;;* description: Закрузчик одной фотографии
;;*
;;**************************************************************************************************

(def app-init {:image nil})

(defn value [app]
  (get app :image))

(defn set-value! [app v]
  (assoc app :image v))

(defn component [app own {:keys [class+]
                          :as   opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-upload (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-upload]}]
      (dom/div #js {:className class+}
               (om/build file-uploder/component app
                         {:opts (merge opts
                                       {:accept     "image/gif, image/jpeg, image/png, image/*"
                                        :update-fn  #(put! chan-upload 1)
                                        :success-fn #(om/update! app :image %)
                                        })})

               (dom/div
                #js {:className "well well-sm"
                     :style     #js {:marginTop 4
                                     :display   "inline-block"}}
                (let [image (@app :image)]
                  (if (empty? image)
                    (glyphicon/render "camera" "" "8em")
                    (media-object/render {:class+ "img-rounded"
                                        :style  #js {:maxWidth 300}
                                        :src    (@app :image)}))))
               ))))

(defn component-form-group  [app owner {:keys [label
                                               label-class+
                                               input-class+
                                               spec-one-image-uploader]
                                        :or   {label                   "Метка"
                                               label-class+ common-form/label-class
                                               input-class+ common-form/input-class
                                               spec-one-image-uploader {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+}
                        (om/build component app {:opts spec-one-image-uploader})
                        (om/build helper-p/component app {}) )))))

;; END Upload one image
;;..................................................................................................
