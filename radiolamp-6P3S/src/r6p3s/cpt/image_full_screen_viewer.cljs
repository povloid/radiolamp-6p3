(ns r6p3s.cpt.image-full-screen-viewer
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.ui.glyphicon :as glyphicon]            
            [r6p3s.core :as c]))


;;**************************************************************************************************
;;* BEGIN Full screen image viewer
;;* tag: <full screen image viewer>
;;*
;;* description: Просмоторщик картинок в полный экран
;;*
;;**************************************************************************************************


(defonce chan-show (chan))

                                        ;TODO: Вынести данную констату в общий файл cljc !!!!!
(def component-id "thumb-show-in-full-screen")


(defonce app-state
  (atom {:src         nil
         :description nil :top_description nil
         :zoom?       false
         :deg         0}))

(defn- component [app]
  (reify
    om/IWillMount
    (will-mount [this]
      (go
        (while true
          (let [{:keys [path src title top_description description deg]
                 :or   {deg (@app :deg)}}
                (<! chan-show)]
            (println "<<[SHOW FULL SCREEN PHOTO]>>")
            (om/transact!
             app #(assoc % :src (or path src)
                         :deg deg
                         :descrioption description
                         :top_description (or top_description title)))))))
    om/IRender
    (render [_]
      (when-let [src (@app :src)]
        (let [{:keys [deg zoom?]} @app
              deg-2               (str "rotate(" deg "deg)")]
          (dom/div
           #js {:className "image-full-screen-viewer"}
           (dom/div #js {:style #js {:position "fixed" :left 0 :top 0 :zIndex 3005}}
                     (dom/button #js {:className ""
                                      :onClick   #(om/transact! app :zoom? not)}
                                 (glyphicon/render (if zoom? "zoom-out" "zoom-in") "" "3em"))
                     (dom/button #js {:className ""
                                      :onClick   #(om/transact!
                                                   app :deg (fn [deg]
                                                              (let [deg (+ deg 90)]
                                                                (if (> deg 270) 0 deg))))}
                                 (glyphicon/render "retweet" "" "3em")))

            (dom/button #js {:className ""
                             :style     #js {:position "fixed" :right 0 :top 0 :zIndex 3005}
                             :onClick   (fn []
                                          (om/transact!
                                           app #(assoc % :src nil :descrioption nil
                                                       :top_description nil))
                                          1)}
                        (glyphicon/render "remove" "" "3em"))
           
           (dom/div
            #js {:style #js {:position "fixed" :zIndex          3000
                             :top      0       :bottom          0 :left 0 :right 0
                             :overflow "auto"  :backgroundColor "black"}}

            

            (dom/img #js {:src   src
                          :style #js {:msTransform     deg-2
                                      :WebkitTransform deg-2
                                      :transform       deg-2
                                      ;;:height (if (#{90 270} deg) (if zoom? nil (.-innerWidth js/window)) nil)
                                      :width           (if zoom? nil (.-innerWidth  js/window))}}))))))))


;; Инициализация
(def component--tag (c/by-id component-id))

(if component--tag
  (do
    (om/root
     component
     app-state
     {:target component--tag :opts {}})
    (println "Компонент FULL SCREEN найден и проинициализирован."))
  (println "Компонент FULL SCREEN НЕ найден по id ="
           component-id " и не проинициализирован."))


;; END Full screen image viewer
;;..................................................................................................
