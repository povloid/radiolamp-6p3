(ns r6p3s.complex.dataform
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.common-input :as common-input]
            [r6p3s.core :as rc]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.toggle-button :as toggle-button]
            [r6p3s.cpt.select :as select]
            [r6p3s.cpt.textarea :as textarea]))



(defn make-app-init [rbs-scheme]
  {:rbtype (->> rbs-scheme
                :realtype seq
                (map (fn [[id {keyname :text}]]
                       {:id (name id) :keyname keyname}))
                (sort-by :keyname)
                (select/list-set! select/app-init))
   :fields (->> rbs-scheme
                :fields seq
                (reduce
                 (fn [a [rbtype {:keys [type text]}]]
                   (assoc a rbtype
                          (condp = type
                            :money   input/app-init
                            :integer input/app-init
                            :boolean toggle-button/app-init
                            :string  input/app-init
                            :text    textarea/app-init
                            :rbs     select/app-init
                            nil)))
                 {}))})


(defn fill [app row rb-ks {:keys [realtype-field] :as rbs-scheme}]
  (-> app
      (update-in [:fields]
                 (fn [app]
                   (let [rb-data (get-in row rb-ks)]
                     (->> rbs-scheme
                          :fields seq
                          (reduce
                           (fn [app [field-k {:keys [type rbtype text rbentity]}]]
                             (let [v (row field-k)]
                               ;;(println "restore: " field-k " = " v)
                               (condp = type
                                 :money   (update-in app [field-k] input/set-value! v)
                                 :integer (update-in app [field-k] input/set-value! v)
                                 :string  (update-in app [field-k] input/set-value! v)
                                 :text    (update-in app [field-k] textarea/set-value! v)
                                 :boolean (update-in app [field-k] toggle-button/set-value! v)
                                 :rbs     (update-in app [field-k]
                                                     (fn [app]
                                                       (-> app
                                                           (select/list-set!
                                                            (vec (get-in rb-data [rbentity rbtype] [])))
                                                           (select/selected-set! v))))
                                 app)))
                           app)))))
      (update-in [:rbtype] select/selected-set! (when-let [rt (row realtype-field)] (name rt)))))



(defn save [row app {:keys [realtype-field] :as rbs-scheme}]
  (let [rbtype-key :realtype
        {:keys [rbtype fields]} app]
    (-> (->> rbs-scheme
             :fields seq
             (reduce
              (fn [row [field-k {:keys [type rbtype text rbentity]}]]
                (let [v (fields field-k)]
                  ;;(println "save: " field-k " = " v)
                  (condp = type
                    :money   (->> v input/value rc/parse-number-or-nil (assoc row field-k))
                    :integer (->> v input/value rc/parse-number-or-nil (assoc row field-k))
                    :string  (->> v input/value (assoc row field-k))
                    :text    (->> v textarea/value (assoc row field-k))
                    :boolean (->> v toggle-button/value rc/parse-boolean (assoc row field-k))
                    :rbs     (->> v select/selected-int-or-nil (assoc row field-k))
                    row)))
              row))
        (assoc realtype-field (when-let [rt (select/selected rbtype)] (keyword rt))))))





(defn component [app own {:keys [rbs-scheme]}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})


    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div #{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                 :style     #js {:padding 0}}

               (om/build select/component-form-group
                         (app :rbtype)
                         {:opts {:label "Тип"}})


               (let [realtype-k (->> @app :rbtype select/selected keyword)
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
