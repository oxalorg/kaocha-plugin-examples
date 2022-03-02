(ns hello-test
  (:require [clojure.test :as t :refer [deftest is]]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.repl :as k]))

(deftest hello-1
  (is (= 1 1)))

(deftest hello-2
  (is (= 2 2)))

(deftest hello-3
  (is (= 3 3)))

(defn un-sort-recursive [test-plan]
  (if-let [tests (:kaocha.test-plan/tests test-plan)]
    (if (= (:kaocha.testable/type test-plan) :kaocha.type/ns)
      (assoc test-plan
             :kaocha.test-plan/tests
             (->> tests
                  (sort-by #(-> %
                                :kaocha.testable/meta
                                :line))))
      (assoc test-plan
             :kaocha.test-plan/tests
             (->> tests
                  (map un-sort-recursive))))
    test-plan))

(defn un-sort-exclude [test-plan]
  (if-let [tests (:kaocha.test-plan/tests test-plan)]
    (if-let [dont-randomize? (:dont-randomize? test-plan)]
      (un-sort-recursive test-plan)
      (assoc test-plan
             :kaocha.test-plan/tests
             (->> tests
                  (map un-sort-exclude))))
    test-plan))

(defplugin hello-test/randomize-exclude
  (post-load [test-plan]
    (def tt test-plan)
    (println "Post loading: " test-plan)
    test-plan))

(defn find-order [test-plan]
  (loop [tp test-plan
         order []]
    (if-let [tests (:kaocha.test-plan/tests tp)]
      (if (= (:kaocha.testable/type tp) :kaocha.type/ns)
        (recur tests (concat order (map #(-> % :kaocha.testable/meta :name) tests)))
        (recur tests (mapcat find-order tests)))
      order)))

(comment
  ;; run to populate `tt`
  (k/run)

  (find-order tt)
  ;; => (hello-3 hello-1 hello-2)

  (find-order (un-sort-exclude tt))
  ;; => (hello-1 hello-2 hello-3)

  ,)
