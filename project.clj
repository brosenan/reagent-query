(defproject brosenan/reagent-query "0.3.0-SNAPSHOT"
  :description "A helper library for testing Reagent code as pure functions "
  :url "https://github.com/brosenan/reagent-query"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.671"]
                 [lein-doo "0.1.7"]
                 [devcards "0.2.3"]]
  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.6"]
            [lein-doo "0.1.7"]]
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.3"]
                                  [im.chit/lucid.publish "1.3.13"]]}}

  :cljsbuild {
              :test-commands {"test" ["lein" "doo" "phantom" "test" "once"]}
              :builds [{:id "dev"             ; development configuration
                        :source-paths ["src"] ; Paths to monitor for build
                        :figwheel true        ; Enable Figwheel
                        :compiler {:main reagent-query.core     ; your main namespace
                                   :asset-path "cljs/out"                       ; Where load-dependent files will go, mind you this one is relative
                                   :output-to "resources/public/cljs/main.js"   ; Where the main file will be built
                                   :output-dir "resources/public/cljs/out"      ; Directory for temporary files
                                   :source-map-timestamp true}                  ; Sourcemaps hurray!
                        }
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:main runners.doo
                                   :optimizations :none
                                   :output-to "resources/public/cljs/tests/all-tests.js"}}
                       {:id "devcards-test"
                        :source-paths ["src" "test"]
                        :figwheel {:devcards true}
                        :compiler {:main runners.browser
                                   :optimizations :none
                                   :asset-path "cljs/tests/out"
                                   :output-dir "resources/public/cljs/tests/out"
                                   :output-to "resources/public/cljs/tests/all-tests.js"
                                   :source-map-timestamp true}}]}
  :main ^:skip-aot reagent-query.core
  :target-path "target/%s"
  
  :publish {:template {:site "reagent-query"
                       :output "docs"
                       :author "Boaz Rosenan"
                       :url "https://github.com/brosenan/reagent-query"}
            :theme "bolton" ;; stark is the default
            :files {"core"
                    {:input "test/reagent_query/core_test.cljs"
                     :title "reagent-query"
                     :subtitle "Test helpers for reagent code"}}})

;; echo "(use 'lucid.publish) (copy-assets) (publish-all)" | lein repl
