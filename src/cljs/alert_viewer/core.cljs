(ns alert-viewer.core
  (:require [ajax.core :refer [GET POST]]
            [cljs-time.format :as tfmt]
            [om-bootstrap.grid :as grid]
            [om-bootstrap.nav :as n]
            [om-bootstrap.panel :as p]
            [om-bootstrap.table :as t]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]))

(enable-console-print!)

(defonce app-state (atom {:text "Alert Viewer"
                          :stats nil
                          :alerts nil}))

(defn ajax-call [url callback]
  (GET url
       {:response-format :json
        :keywords? true
        :handler callback}))



(defn ajax-post [url params callback]
  (POST url
        {:response-format :json
         :format :json
         :keywords? true
         :handler callback
         :params params}))


(defn set-alerts [response]
  (swap! app-state assoc :alerts (get-in response [:hits :hits])))


(defn set-stats [response]
  (swap! app-state assoc :stats response))


(defn current-alerts []
  (ajax-post "http://localhost:9250/.watch_history*/_search"
             {:size 0
              :aggs {:current-alerts {:terms {:field :watch_id}}}}
             #(swap! app-state assoc :alerts
                     (get-in % [:aggregations :current-alerts :buckets]))))


(defn alert-history [id cb]
  (ajax-post "http://localhost:9250/.watch_history*/_search"
             {:filter {:term {:watch_id id}}}
             cb))

(def app-root (. js/document (getElementById "app")) )
(def nav-root (. js/document (getElementById "navbar")))

(declare build-main)


(defn printable-datetime [dt]
  (tfmt/unparse
   (tfmt/formatter "MM/dd/YYYY hh:mm:ss a")
   (tfmt/parse (tfmt/formatters :date-time) dt)))


(defcomponent alert-history-item
  [{{{{:keys [met status]}     :condition
       :keys [execution_time]} :result} :_source} _]
  (render [_]
    (dom/tr
     (dom/td (printable-datetime execution_time))
     (dom/td (if met "+"))
     (dom/td
      (dom/a {:href "#"} "raw")))))


(defcomponent alert-detail [{:keys [history]} _]
  (render [_]
    (dom/div
     (t/table {}
      (dom/thead
       (dom/tr
        (dom/th "Run On")
        (dom/th "Matched?")
        (dom/th "")))
      (dom/tbody
       (om/build-all alert-history-item history)))
     (dom/a {:href "#"
             :onClick #(build-main)} "Back"))))


(defcomponent alert-list-item [{:keys [key] :as alert} owner]
  (render [_]
    (dom/li
     (dom/a {:href "#"
             :onClick #(alert-history
                        key
                        (fn [r]
                          (om/root alert-detail
                                   {:history (get-in r [:hits :hits])}
                                   {:target app-root})))}
            key))))

(defcomponent stats-summary [{:keys [watcher_state]} _]
  (render [_]
    (dom/div {:class "panel"}
             (dom/h3 {:class "subheader"} "Engine Status")
             (dom/h4 watcher_state))))

(defcomponent alert-list [{:keys [text alerts stats]} owner]
  (render [_]
    (dom/div
     (grid/grid
      {}
      (grid/row
       {}
       (grid/col {:md 6}
                 (p/panel {:header (dom/h2 "Active Alerts")}
                          (dom/ul
                           (om/build-all alert-list-item alerts))))
       (grid/col {:md 6}
                 (p/panel {:header (dom/h2 "Stats")}
                          (om/build stats-summary stats))))))))


(defcomponent nav [_ _]
  (render [_]
    (n/navbar {:inverse? true
               :brand (dom/a {:href "#"} "Alert Viewer")})))

(defn build-nav []
  (om/root nav app-state {:target nav-root}))

(defn build-main []
  (om/root alert-list app-state {:target app-root}))


(defn main []
  (current-alerts)
  (build-nav)
  (build-main))
