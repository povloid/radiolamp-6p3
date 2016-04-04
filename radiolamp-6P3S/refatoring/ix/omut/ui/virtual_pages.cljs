(ns ix.omut.ui.virtual-pages
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.ui.button :as button]))





;;**************************************************************************************************
;;* BEGIN Virtual pages
;;* tag: <virtual pages>
;;*
;;* description: Виртуальные страници для нескольких связанных форм
;;*
;;**************************************************************************************************

(def app-init
  {:current :main})

(defn current [app]
  (app :current))

(defn go-to-page!! [app page]
  (om/update! app :current page))

(defn render
  ([app page-key body]
   (render app page-key nil body))
  ([app page-key back-key body]
   (dom/div #js {:style #js {:display
                             (if (= (current @app) page-key)
                               "" "none") }}
            (when back-key
              (button/render
               {:type     :default
                :on-click (fn [_]
                            (go-to-page!! app back-key)
                            1)
                :text     (dom/span nil
                                    (dom/span #js {:className   "glyphicon glyphicon-backward"
                                                   :aria-hidden "true"})
                                    " Назад")
                }))
            body)))

;; END Virtual pages
;;..................................................................................................
