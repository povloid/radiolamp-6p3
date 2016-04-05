(ns r6p3s.ui.button
  (:require [om.dom :as dom :include-macros true]))



                                        ;TODO: Дублируется пока из core.cljs для разрешения цикулярных зависимоствей при рефакторинге !!!
;TODO: В будущем надо оставить одну функцию из core.cljs
(defn on-click-com-fn [f]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (f)
    1))



(defn render [{:keys [text
                      type
                      size
                      block?
                      disabled?
                      active?
                      on-click
                      style]
               :or   {text "Кнопка"
                      type :default}}]
  (dom/button #js {:className  (str "btn "
                                    ({:default "btn-default"
                                      :primary "btn-primary"
                                      :success "btn-success"
                                      :info    "btn-info"
                                      :warning "btn-warning"
                                      :danger  "btn-danger"
                                      :link    "btn-link"} type)
                                    (if size ({:lg " btn-lg"
                                               :sm " btn-sm"
                                               :xs " btn-xs"} size)
                                        "")
                                    (if block? " btn-block" "")
                                    (if active? " active" "")
                                    )
                   :type       "button"
                   :disabled   (if disabled? "disabled" "")
                   :onClick    (on-click-com-fn on-click)
                   :onTouchEnd (on-click-com-fn on-click)
                   :style      style
                   }
              text))
