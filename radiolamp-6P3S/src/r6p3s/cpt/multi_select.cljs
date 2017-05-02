(ns r6p3s.cpt.multi-select
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.cpt.tbody-trs-sel :as tbody-trs-sel]))




(def app-init
  {:data []})

(defn selected [app]
  (filter (comp :selected :omut-row)))


(defn component [app own {:keys [on-change-fn title-field-key on-select-fn]
                          :or   {title-field-key :keyname}}]
  (reify
    om/IInitState
    (init-state [_]
      {:show-popup? false})

    om/IRenderState
    (render-state [_ {:keys [show-popup?]}]
      (letfn [(open []
                (om/set-state! own :show-popup? true))

              (close []
                (om/set-state! own :show-popup? false))]
        (dom/div nil
                 (button/render {:text     "..."
                                 :active?  show-popup?
                                 :on-click (fn []
                                             (if show-popup?
                                               (close)
                                               (open)))
                                 :style    #js {:float "right"}})

                 (let [data (->> @app
                                 :data
                                 (filter (comp :selected :omut-row)))]
                   (if (empty? data)
                     (dom/div #js {:className "text-muted"} "ничего не выбрано")
                     (->> data
                          (map title-field-key)
                          (clojure.string/join ", ")
                          (apply dom/div #js {:className "text-primary"}))))

                 (dom/div
                  #js {:style #js {:display    (if show-popup? "" "none")
                                   :position   "absolute"
                                   :width      "100%"
                                   :top        42
                                   :zIndex     10
                                   :boxShadow  "0px 2px 8px"
                                   :background "white"}}

                  (table/render {:hover?      true
                                 :bordered?   true
                                 :striped?    true
                                 :responsive? false
                                 :tbody
                                 (om/build tbody-trs-sel/component (:data app)
                                           {:opts {:selection-type :multi
                                                   :app-to-tds-seq-fn
                                                   (fn [row]
                                                     [(dom/td nil (row title-field-key))])
                                                   :on-select-fn
                                                   (fn [row]
                                                     (when on-select-fn
                                                       (on-select-fn row)))
                                                   }})
                                 })

                  #_(dom/div #js {:className "btn-group"
                                  :style     #js {:float  "right"
                                                  :margin 4}}
                             (button/render
                              {:type     :default
                               :on-click close
                               :text     (dom/span nil
                                                   (glyphicon/render "remove")
                                                   " Закрыть")}))))))))





(defn component-form-group  [app _ {:keys [label
                                           type
                                           label-class+
                                           input-class+
                                           spec-select]
                                    :or   {label        "Метка"
                                           label-class+ common-form/label-class
                                           input-class+ common-form/input-class
                                           spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-select})

                        (om/build helper-p/component app {}))))))
