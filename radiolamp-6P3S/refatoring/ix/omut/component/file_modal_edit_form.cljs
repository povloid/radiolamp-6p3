(ns ix.omut.component.file-modal-edit-form
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.file-edit-form :as file-edit-form]
            [ix.omut.component.modal-edit-form-for-id--yes-no :as modal-edit-form-for-id--yes-no]))



(def app-init
  (merge modal-edit-form-for-id--yes-no/app-init file-edit-form/app-init))


(defn component [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--yes-no/component app
                {:opts (assoc opts :edit-form-for-id file-edit-form/component)}))))
