(ns r6p3s.cpt.images-gallery-1
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.cpt.image-full-screen-viewer :as image-full-screen-viewer]
            [goog.net.ImageLoader :as ImageLoader]
            [goog.net.EventType]
            [goog.events :as events]))



(defn component [app own]
  (letfn [(img-setup []
            (om/update-state!
             own (fn [{:keys [div-id img-id deg] :as state}]
                   (let [div (c/by-id div-id)
                         img (c/by-id img-id)]
                     (when (and div img)
                       (let [r?    (or (= deg 90) (= deg 270))
                             div-w (.-clientWidth  div)
                             div-h (.-clientHeight div)
                             img-w (.-clientWidth  img)
                             img-h (.-clientHeight img)
                             fat?  (>= img-w img-h)]
                         (assoc state
                                :img-margin-top (when r?
                                                  (if fat?
                                                    (* 0.5 (- img-w img-h))
                                                    nil))
                                :div-height (when r?
                                              (if fat?
                                                (+ 12 img-w)
                                                nil))
                                :img-height (when (and r? (not fat?))
                                              div-w))))))))   
            ]
    
    (reify

      om/IInitState
      (init-state [_]
        {:i             0                 :deg            0
         :div-id        (c/uniq-id "div") :img-id         (c/uniq-id "img")
         :div-height    nil               :img-margin-top nil
         :img-height    nil
         :src           "/images/preloader_1_128.gif"
         :chan-img-load (chan)})


    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-img-load img-id]} (om/get-state own)]

        (go ;; Обработка отрисовки табулятороров и переключения на страници
          (while true
            (let [i            (<! chan-img-load)
                  src          (get-in @app [i :path] "")
                  image-loader (new goog.net.ImageLoader)]            
              (println "update image:" src)
              ;; Сначала ставим гифку прогресса
              (om/set-state! own :src "/images/preloader_1_128.gif")
              ;; Теперь по загрузчику
              (events/listen image-loader goog.net.EventType.COMPLETE
                             (fn []
                               (println "loaded.")
                               (om/update-state!
                                own (fn [state]
                                      (assoc state :src src :i i)))
                               (img-setup)))
              
              (.addImage image-loader "current-image" src)            
              (.start image-loader))))

        (put! chan-img-load 0)))

      
      om/IDidUpdate
      (did-update [_ next-props next-state]
        (when-not (= @app next-props)
          (let [{:keys [chan-img-load]} (om/get-state own)]
            (put! chan-img-load 0))))

      om/IWillReceiveProps
      (will-receive-props [_ next-props]
        (om/set-state! own :i 0))

      om/IRenderState
      (render-state [_ {:keys [i deg div-id div-height img-id img-margin-top img-height
                               chan-img-load src]}]
        (let [deg-2 (str  "rotate(" deg "deg)")]
          (if (empty? @app)
            (dom/h2 nil "Изображений нет")
            (dom/div
             #js {}
             (dom/div nil
                      (dom/b #js {:style #js {:fontSize "24px"}} "Изображение " (inc i) " из " (count @app))


                      (dom/div #js {:className "btn-toolbar" :style #js {:float "right"}}
                               (dom/div #js {:className "btn-group"}
                                        (button/render
                                         {:text (glyphicon/render "retweet")
                                          :type :default
                                          :size :lg
                                          :on-click
                                          (fn []
                                            (om/update-state!
                                             own :deg
                                             #(let [deg (+ % 90)]
                                                (if (> deg 270) 0 deg)))
                                            (img-setup))})
                                        (dom/div #js {:className "btn-group"}
                                                 (button/render
                                                  {:text     (glyphicon/render "fullscreen")
                                                   :type     :default
                                                   :size     :lg
                                                   :on-click (fn []
                                                               (put! image-full-screen-viewer/chan-show
                                                                     {:src src :deg deg})
                                                               i)})))

                               (dom/div #js {:className "btn-group"}
                                        (button/render {:text     (glyphicon/render "chevron-left")
                                                        :type     :default
                                                        :size     :lg
                                                        :on-click (fn [_]
                                                                    (put! chan-img-load
                                                                          (let [i (dec i)]
                                                                            (if (< i 0) (dec (count @app)) i)))
                                                                    1)})
                                        (button/render {:text     (glyphicon/render "chevron-right")
                                                        :type     :default
                                                        :size     :lg
                                                        :on-click (fn [_]
                                                                    (put! chan-img-load
                                                                          (let [i (inc i)]
                                                                            i (if (= i (count @app)) 0 i)))
                                                                    1)}))))
             (dom/br nil)
             (dom/br nil)
             (dom/div
              #js {:id div-id :className "thumbnail" :style #js {:height div-height}}
              (dom/img #js {:id        img-id
                            :className ""
                            :src       src
                            :style     #js {:msTransform     deg-2
                                            :WebkitTransform deg-2
                                            :transform       deg-2
                                            :marginTop       img-margin-top
                                            :height          img-height}
                            :onClick   (fn []
                                         (put! image-full-screen-viewer/chan-show {:src src :deg deg})
                                         1)
                            :onLoad    img-setup}))
             (dom/h2 nil (get-in @app [i :top_description] ""))
             (dom/p nil (get-in @app [i :description] "")))))))))
