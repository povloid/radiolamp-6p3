(ns r6p3s.cpt.input-change-password
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.helper-p :as helper-p]))





(def app-init
  {:password-1 {:value ""} :password-2 {:value ""} })

(defn clean [_]
  {:password-1 {:value ""} :password-2 {:value ""} })

(defn valid? [a]
  (= (get-in a [:password-1 :value]) (get-in a [:password-2 :value])))

(defn check [a]
  (if (not (valid? a))
    (throw (js/Error. "Пароли в полях не совпадают!"))
    a))

(defn value [a]
  (get-in a [:password-1 :value]))





(defn component [app owner]
  (letfn [(onChange-updated-fn []
            (om/transact!
             app (fn [app]
                   (if (not (valid? app))
                     (assoc app :has-warning? true :text-warning "Пароли не совпадают")
                     (dissoc app :text-warning :has-warning?)))) )]
    (reify
      om/IRender
      (render [this]
        (dom/div
         nil
         (om/build input/component (:password-1 app)
                   {:opts {:type                "password"
                           :onChange-updated-fn onChange-updated-fn
                           :style               #js {:marginBottom 5}
                           :autocomplete        "off"}})
         (om/build input/component (:password-2 app)
                   {:opts {:type                "password"
                           :onChange-updated-fn onChange-updated-fn
                           :autocomplete        "off"}})
         (om/build helper-p/component app {}) )))))






(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               label-class+
                                               spec-input]
                                        :or   {label        "Пароль"
                                               label-class+ common-form/label-class
                                               input-class+ common-form/input-class
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className label-class+}
                        (om/build component app {:opts spec-input}))))))
