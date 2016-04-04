(ns ix.omut.component.toggle-buttons-selector
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [ix.io :as ix-io]
            [ix.net :as ixnet]
            [ix.omut.core :as c]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]

            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]

            [goog.string :as gstring]
            [goog.string.format]
            [ix.omut.ui.button :as button])

  (:import [goog.dom query]))



(defn app-init [buttons]
  {:buttons (vec (map
                  (fn [{:keys [value key] :as b} i]
                    (assoc b
                           :value (if (nil? value) false value)
                           :key key))
                  buttons))})

(defn get-selected [app]
  (->> app :buttons (filter :value) (map :key)))

(defn get-selected-one [app]
  (first (toggle-buttons-selector-get-selected app)))

(defn component [app own {:keys [selection-type
                                                     onClick-fn]}]
  (reify
    om/IRender
    (render [_]
      (->> app
           :buttons
           (map (fn [app-row]
                  (let [{:keys [bs-type value disabled? text]} @app-row]
                    (button/render
                     {:type      (or bs-type :default)
                      :active?   value
                      :disabled? disabled?
                      :on-click
                      (fn [_]
                        (when (= selection-type :one)
                          (om/transact!
                           app :buttons
                           (fn [app] (vec (map #(assoc % :value false) app)))))
                        (om/transact! app-row :value not)
                        (when onClick-fn (onClick-fn)))
                      :text      text}))))
           (apply
            dom/div #js {:className "btn-group" :role "group"})))))

