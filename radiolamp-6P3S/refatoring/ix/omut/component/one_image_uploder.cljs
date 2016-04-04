(ns ix.omut.component.one-image-uploder
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.component.file-uploder :as file-uploder]
            [ix.omut.core :as c]
            [ix.omut.component.common-input :as common-input]
            [ix.omut.component.helper-p :as helper-p]))


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

(defn value-set! [app v]
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
                    (c/ui-glyphicon "camera" "" "8em")
                    (c/ui-media-object {:class+ "img-rounded"
                                        :style  #js {:maxWidth 300}
                                        :src    (@app :image)}))))
               ))))

(defn component-form-group  [app owner {:keys [label
                                               label-class+
                                               input-class+
                                               spec-one-image-uploader]
                                        :or   {label                   "Метка"
                                               label-class+            "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                               input-class+            "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                               spec-one-image-uploader {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+}
                        (om/build component {:opts spec-one-image-uploader})
                        (om/build helper-p/component app {}) )))))

;; END Upload one image
;;..................................................................................................
