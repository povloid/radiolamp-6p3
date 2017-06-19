(ns r6p3s.complex.areal-modal-edit-form
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.cpt.modal-edit-form-for-id--yes-no :as modal-edit-form-for-id--yes-no]
            [r6p3s.complex.areal-edit-form :as edit-form]))


(def app-init
  (merge modal-edit-form-for-id--yes-no/app-init edit-form/app-init))

(defn component [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--yes-no/component app
                {:opts (assoc opts
                              :edit-form-for-id edit-form/component
                              :new-or-edit-fn?
                              (fn [_]
                                (if (-> @app :id nil?)
                                  :new :edit)))}))))
