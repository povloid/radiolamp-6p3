(ns ix.omut.component.message-modal
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.modal :as modal])
  (:import [goog.dom query]))


(defonce message-modal-app
  (atom (assoc modal/app-init
               :title
               :message "")))

(defn- cursor-message-root [] (om/root-cursor message-modal-app))

(def message-modal-id "message-modal")

(defn component [app _]
  (reify
    om/IRender
    (render [_]
      (let [[type-class+ title-default icon]
            (get-in {:muted   ["text-muted"   "Текстовое сообщение"
                               nil]
                     :primary ["text-primary" "Предложение"
                               nil]
                     :success ["text-success" "Операция проведени успешно"
                               "ok"]
                     :info    ["text-info"    "Информация"
                               "info"]
                     :warning ["text-warning" "Внимание!"
                               "alert"]
                     :danger  ["text-danger"  "Ошибка!!!"
                               "exclamation-sign"]
                     } [(get-in @app [:type] :muted)])]

        (om/build modal/component app
                  {:opts {:header (dom/h1 #js {:className type-class+}
                                          (when icon (c/ui-glyphicon icon "1em"))
                                          (or (@app :title) title-default))
                          :body   (dom/p #js {:className type-class+}
                                         " " (@app :message))}})))))


(defn show-in-message-modal [type {:keys [title message] :as message-row}]
  (let [_ (or (c/by-id message-modal-id)
              (let [error-div (.createElement js/document "div")
                    tag-body  (aget (query "body") 0)]
                (set! (.-id error-div) message-modal-id)
                (.appendChild tag-body error-div)

                (om/root component
                         message-modal-app
                         {:target (c/by-id message-modal-id)})

                error-div))]

    (om/transact! (cursor-message-root)
                  (fn [app]
                    (assoc app
                           :title title
                           :message (or message (str message-row))
                           :type type)))
    (modal/show (cursor-message-root))))

(def show-in-message-modal-muted   (partial show-in-message-modal :muted))
(def show-in-message-modal-primary (partial show-in-message-modal :primary))
(def show-in-message-modal-success (partial show-in-message-modal :success))
(def show-in-message-modal-info    (partial show-in-message-modal :info))
(def show-in-message-modal-warning (partial show-in-message-modal :warning))
(def show-in-message-modal-danger  (partial show-in-message-modal :danger))
