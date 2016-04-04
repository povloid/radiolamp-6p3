(ns ix.omut.component.nav
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]            
            [ix.omut.core :as c]
            [ix.omut.ui.nav :as nav]
            [ix.omut.ui.navbar-li :as navbar-li]            
            [ix.omut.ui.navbar-li-dropdown :as navbar-li-dropdown]
            [ix.omut.ui.ul-navbar-nav-right :as ul-navbar-nav-right]
            [ix.omut.ui.ul-navbar-nav :as ul-navbar-nav]
            [ix.omut.ui.navbar-li-separator :as navbar-li-separator]))



(def nav-app-state-key :menu)




(defn component [app _ opts]
  (letfn [(f1 [{:keys [sub separator?] :as row}]
            (if separator?
              (navbar-li-separator/render)
              (if (coll? sub)
                (apply (partial navbar-li-dropdown/render row) (map f1 sub))
                (navbar-li/render row))))]
    (reify
      om/IRender
      (render [_]
        (let [m (nav-app-state-key app)]
          (nav/render opts
                  (when-let [menus (:left m)]
                    (apply ul-navbar-nav/render
                           (map f1 menus)))
                  (when-let [menus (:right m)]
                    (apply ul-navbar-nav-right/render
                           (map f1 menus)))
                  ))))))
