(ns r6p3s.cpt.file-modal-edit-form
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.file-edit-form :as file-edit-form]
            [r6p3s.cpt.modal-edit-form-for-id--yes-no :as modal-edit-form-for-id--yes-no]))



(def app-init
  (merge modal-edit-form-for-id--yes-no/app-init file-edit-form/app-init))


(defn component [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--yes-no/component app
                {:opts (assoc opts :edit-form-for-id file-edit-form/component)}))))
