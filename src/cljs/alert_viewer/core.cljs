(ns alert-viewer.core
  (:require [ajax.core :refer [GET POST]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [cljs-time.format :as tfmt]))

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

(def app-root (. js/document (getElementById "app")) )


(declare build-main)


(defcomponent alert-detail [{{{:keys [last_checked last_met_condition]}
                              :_status} :_source
                              :keys [_id]} _]
  (render [_]
    (dom/div
     (dom/h2 {:class "subheader"} "Alert Detail")
     (dom/p _id)
     (dom/p last_checked)
     (dom/p last_met_condition)
     (dom/a {:href "#"
             :onClick #(build-main)} "Back"))))


(defcomponent alert-list-item [{:keys [key] :as alert} owner]
  (render [_]
    (dom/li
     (dom/a {:href "#"
             :onClick #(om/root alert-detail alert {:target app-root})}
            key))))

(defcomponent stats-summary [{:keys [watcher_state]} _]
  (render [_]
    (dom/div {:class "panel"}
             (dom/h3 {:class "subheader"} "Engine Status")
             (dom/h4 watcher_state))))

(defcomponent alert-list [{:keys [text alerts stats]} owner]
  (render [_]
    (dom/div
     (dom/h2 "Alert Viewer")
     (dom/div {:class "large-6 columns"}
       (dom/div {:class "panel"}
                (dom/h3 {:class "subheader"} "Active Alerts")
                (dom/ul
                 (om/build-all alert-list-item alerts))))
     (dom/div {:class "large-6 columns"}
              (om/build stats-summary stats)))))


(defn build-main []
  (om/root alert-list app-state {:target app-root}))


(defn main []
  (current-alerts)
  (build-main))
