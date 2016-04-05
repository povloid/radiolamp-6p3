(ns ix.omut.ui.media
  (:require [om.dom :as dom :include-macros true]
            [ix.omut.ui.button :as button]))


(defn render [{:keys [media-object
                      heading heading-2
                      on-click-fn
                      on-click-in-image-fn
                      href
                      style
                      body
                      button-do-fn
                      button-do-text
                      heading-tag
                      ]}]
  (dom/div #js {:className "media" :style style
                :onClick   on-click-fn}
           (dom/div #js {:className "media-left"}
                    (dom/a #js {:href    href
                                :onClick (fn [e]
                                           (when on-click-in-image-fn (on-click-in-image-fn))
                                           ;; Далее прервать выполнение события для родительского
                                           ;; компонента
                                           (.stopPropagation e))}
                           media-object))
           (dom/div #js {:className "media-body"}
                    (when button-do-fn (button/render {:style    #js {:float "right"}
                                                       :type     :primary
                                                       :text     (or button-do-text "Действие")
                                                       :on-click button-do-fn}))
                    (when href (dom/a #js {:style #js {:float "right"}
                                           :href  (or href "#") :target "_blank"}
                                      (dom/button #js {:className "btn btn-success"} "скачать")))
                    (when heading ((or heading-tag dom/h4) #js {:className "media-heading"} heading
                                   (when heading-2 (dom/small nil " - " heading-2 ))))
                    body)))








