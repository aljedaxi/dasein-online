(defproject stasis-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring "1.7.1"]
                 [org.clojure/data.json "2.4.0"]
                 [hiccup "2.0.0-RC1"]
                 [optimus "2023-02-08"]
                 [markdown-clj "1.11.4"]
                 [markdown-to-hiccup "0.6.2"]
                 [stasis "2023.06.03"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler stasis-test.core/app}
  :repl-options {:init-ns stasis-test.core})
