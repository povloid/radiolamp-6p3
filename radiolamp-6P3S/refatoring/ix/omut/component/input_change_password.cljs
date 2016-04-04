(ns ix.omut.component.change-password
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.common-input :as common-input]
            [ix.omut.component.input :as input]
            [ix.omut.component.helper-p :as helper-p]))





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
         (om/build input/component (:password-1 app) {:opts {:type                "password"
                                                             :onChange-updated-fn onChange-updated-fn}})
         (om/build input/component (:password-2 app) {:opts {:type                "password"
                                                             :onChange-updated-fn onChange-updated-fn}})
         (om/build helper-p/component app {}) )))))





(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               class+
                                               spec-input]
                                        :or   {label        "Пароль"
                                               label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                               class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className class+}
                        (om/build component app {:opts spec-input}))))))
