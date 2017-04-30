(ns r6p3s.complex.dataform-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.common-input :as common-input]
            [r6p3s.core :as rc]
            [r6p3s.ui.panel-with-table :as panel-with-table]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.toggle-button :as toggle-button]
            [r6p3s.cpt.select :as select]
            [r6p3s.cpt.textarea :as textarea]))



(def app-init
  {})

(defn component
  "Визуальный компонент для формирвоания окна поиска по форме"
  [app own {:keys [rbs-scheme]}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})


    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div #{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                 :style     #js {:padding 0}}

               (dom/h1 nil "привет")

               #_(om/build select/component-form-group
                           (app :rbtype)
                           {:opts {:label "Тип"}})


               #_(let [realtype-k (->> @app :rbtype select/selected keyword)
                       app        (app :fields)
                       show       (into
                                   (get-in rbs-scheme [:common :fields] #{})
                                   (get-in rbs-scheme [:realtype realtype-k :fields] #{}))]

                   (println realtype-k show)

                   (->> rbs-scheme
                        :fields seq
                        (filter (comp show first))
                        (map
                         (fn [[rbtype {:keys [type text min max]}]]
                           (condp = type
                             :money   (om/build input/component-form-group
                                                (app rbtype)
                                                {:opts {:label      text
                                                        :spec-input {:type "number"
                                                                     :min  min}}})
                             :integer (om/build input/component-form-group
                                                (app rbtype)
                                                {:opts {:label      text
                                                        :spec-input {:type "number"
                                                                     :min  min}}})
                             :string  (om/build input/component-form-group
                                                (app rbtype)
                                                {:opts {:label text}})
                             :text    (om/build textarea/component-form-group
                                                (app rbtype)
                                                {:opts {:label text}})
                             :boolean (om/build toggle-button/component-form-group
                                                (app rbtype)
                                                {:opts {:label text}})
                             :rbs     (om/build select/component-form-group
                                                (app rbtype)
                                                {:opts {:label text}})
                             nil)))
                        (apply dom/div
                               #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                                    :style     #js {:paddingTop    15
                                                    :paddingBottom 15
                                                    :paddingLeft   0
                                                    :paddingRight  0}})))))))
