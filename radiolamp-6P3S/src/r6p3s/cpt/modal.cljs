(ns r6p3s.cpt.modal
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon])

  (:import [goog.dom query]))



;;**************************************************************************************************
;;* BEGIN modal
;;* tag: <modal>
;;*
;;* description: Модальные диалоги
;;*
;;**************************************************************************************************

(def app-init
  {:show false})

(defn show [app]
  (om/transact!
   app :show
   (fn [i] (if i (inc i) 1))))

(defn hide [app]
  (om/transact!
   app :show
   (fn [_] false)))



(defonce modals-status (atom #{}))
(add-watch
 modals-status :log
 (fn [_ _ old new]
   (when (not= old new)
     (println "modals-status: " new)
     (let[tag-body (aget (query "body") 0)]
       (if (empty? new)
         (gdc/remove tag-body "modal-open")
         (gdc/add    tag-body "modal-open")))

     (let [d (clojure.set/difference new old)]
       (when (not (empty? d))
         (let [new-dialog-id (first d)]
           (println "Открылся " new-dialog-id)))))))

(defn component [app owner {:keys [label
                                   modal-size
                                   header
                                   body
                                   footer
                                   class+
                                   on-close-button-fn]
                            :or   {label      "Пустая пометка"
                                   modal-size :default
                                   class+     "" }
                            }]
  (letfn [(close [_]
            (when on-close-button-fn
              (on-close-button-fn))
            (om/update! app :show false)
            1)]
    (reify
      om/IInitState
      (init-state [_]
        {:id (c/uniq-id "modal")})
      om/IWillUnmount
      (will-unmount [_]
        (swap! modals-status disj (om/get-state owner :id)))
      ;; om/IDidUpdate
      ;; (did-update [_ next-props _]
      ;;   (println "IDidUpdate" (om/get-state owner :id) (:show next-props) "->" (:show app))
      ;;   (when (< 1 (count @modals-status))
      ;;     (let [id (om/get-state owner :id)
      ;;           e (.getElementById js/document id)
      ;;           scrollTop (.-scrollTop e)
      ;;           itop (.-innerWidth js/top)
      ;;           ]
      ;;       ;;(.setAttribute e "style" (str "margin-top:"  (.-scrollTop e) "px"))
      ;;       ))
      ;;   )
      om/IWillUpdate
      (will-update [_ next-props _]
        ;;(println "IWillUpdate" (:show next-props) "->" (:show app))
        (when (not (:show next-props))
          (set! (.-scrollTop (.getElementById js/document (om/get-state owner :id))) 0)))
      om/IRenderState
      (render-state [this {id :id}]
        ;;(println "modal id:" id)
        (let [show? (:show app)]
          (swap! modals-status (if show? conj disj) id)
          (dom/div #js {:id              id
                        :aria-hidden     "true"
                        :aria-labelledby label
                        ;; Данное решение пораждает нехорошее поведение
                        ;; например при клике на полноэкранную штуку диалог тоже закрывается
                        ;; вместе с открытыми поддиалогами
                        ;;:onClick         (c/on-click-com-fn close)
                        :style           (if show?
                                           #js {:display         "block"
                                                :paddingLeft     0
                                                :backgroundColor "rgba(0, 0, 0, 0.2)"}
                                           #js {:display "none" })
                        :className       (if show? "modal in" "modal")
                        :role            "dialog"
                        :tabIndex        "-1"}


                   (dom/button #js {:type         "button" :className  "close"
                                    :data-dismiss "modal"  :aria-label "Close"
                                    :style        #js {:position "fixed"
                                                       :top      "10%"
                                                       :right    35
                                                       :zIndex   5
                                                       :fontSize "2.5em"}
                                    :onClick      (fn []
                                                    (set! (.-scrollTop (.getElementById js/document (om/get-state owner :id))) 0))}
                               (glyphicon/render "triangle-top"))

                   (dom/button #js {:type         "button" :className  "close"
                                    :data-dismiss "modal"  :aria-label "Close"
                                    :style        #js {:position "fixed"
                                                       :bottom   "10%"
                                                       :right    35
                                                       :zIndex   5
                                                       :fontSize "2.5em"}
                                    :onClick      (fn []
                                                    (let [e (.getElementById js/document (om/get-state owner :id))]
                                                      (set! (.-scrollTop e) (.-scrollHeight e))))}
                               (glyphicon/render "triangle-bottom"))



                   (dom/div #js {:className (str "modal-dialog"
                                                 (condp = modal-size
                                                   :sm " modal-sm"
                                                   :lg " modal-lg"
                                                   "")
                                                 " " class+)}
                            (dom/div #js {:className "modal-content"}
                                     (dom/div #js {:className "modal-header"}
                                              (dom/button #js {:type         "button" :className  "close"
                                                               :data-dismiss "modal"  :aria-label "Close"
                                                               :onClick      close}
                                                          (glyphicon/render "remove"))

                                              (or header (dom/h4 #js {:className "modal-title"} label)))
                                     (dom/div #js {:className "modal-body"
                                                   :style     #js {:marginRight 40}}

                                              (or body (dom/p #js {:className "text-info"}
                                                              "Пустое пространство диалога. Можно наполнить элементами"))

                                              )
                                     (dom/div #js {:className "modal-footer"}
                                              (or footer (button/render {:type     :default
                                                                         :on-click close
                                                                         :text     "Закрыть"})))))))))))
