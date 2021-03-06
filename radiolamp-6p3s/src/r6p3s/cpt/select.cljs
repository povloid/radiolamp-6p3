(ns r6p3s.cpt.select
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]))


(def no-select-v "NO-SELECT")

(def app-init
  {:selected no-select-v
   :list     []})

(defn list-get [app]
  (app :list))



(defn list-set! [app new-list]
  (assoc app :list new-list))

(defn selected-set! [app selected]
  (assoc app :selected (str selected)))

(defn selected-set-nil! [app]
  (assoc app :selected no-select-v))

(defn selected [app]
  (let [sv (app :selected)]
    (if (or (nil? sv)
            (empty? sv)
            (= sv no-select-v)) nil sv)))


(defn selected-int-or-nil [app]
  (when-let [v (selected app)]
    (js/parseInt v)))


(defn get-selected-row [app & [value-field-key]]
  (let [sel (selected app)]
    ;;(println ">>> " sel " >> " (:list app))
    (->> app
         :list
         (filter (fn [row]
                   (= sel (str (row (or value-field-key :id))))))
         first)))


(defn component [app _ {:keys [first-item-text
                               on-change-fn
                               value-field-key
                               disabled?
                               alert-warn-on-not-selected?
                               title-field-key]
                        :or   {value-field-key :id
                               title-field-key :keyname
                               first-item-text "Выбрать..."}}]
  (reify
    om/IRender
    (render [_]
      ;;(println "SELECT APP:" @app)
      (apply
       dom/select
       #js {:value     (@app :selected)
            :className "form-control"
            :disabled  (when disabled? "disabled")
            :onChange
            (fn [e]
              (let [v (-> e .-target .-value)]
                ;; Для подсветки при пустом значении
                (if (and alert-warn-on-not-selected?
                         (or (nil? v)
                             (= v no-select-v)
                             (empty? v)))
                  (om/transact! app #(assoc % :has-warning? true
                                            :text-warning "Невыбрано значение"))
                  (do (helper-p/clean app)
                      (common-input/input-css-string-has?-clean app)
                      (om/update! app :has-success? true)))
                ;;Дальнейшая отработка действия
                (om/update! app :selected v)
                (when on-change-fn (on-change-fn v))))}

       (doall
        (map (fn [row]
               (dom/option #js {:value (str (value-field-key row))} (str (title-field-key row))))
             (into [{value-field-key no-select-v title-field-key first-item-text}] (@app :list)))) ))))

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
