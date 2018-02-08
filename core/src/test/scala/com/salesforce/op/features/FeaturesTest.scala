/*
 * Copyright (c) 2017, Salesforce.com, Inc.
 * All rights reserved.
 */

package com.salesforce.op.features

import com.salesforce.op._
import com.salesforce.op.features.types._
import com.salesforce.op.test.{PassengerFeaturesTest, _}
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success}


// scalastyle:off
@RunWith(classOf[JUnitRunner])
class FeaturesTest extends WordSpec with PassengerFeaturesTest with TestCommon {

  Spec[Feature[_]] when {
    // will not test all edge cases of equals method. That would be too many combinations
    "equals" should {
      "returns true when comparing a feature after copying" in {
        val f1 = height + weight
        val f2 = f1.asInstanceOf[Feature[Real]].copy()
        f1.equals(f2) shouldBe true
        f2.equals(f1) shouldBe true
      }
      "not be commutative in parent ordering" in {
        val f1 = height + weight
        val f2 = f1.asInstanceOf[Feature[Real]].copy(
          parents = f1.parents.tail :+ f1.parents.head
        )
        f1.equals(f2) shouldBe false
        f2.equals(f1) shouldBe false
      }
      "be false when comparing different features" in {
        age.equals(weight) shouldBe false
        age == weight shouldBe false
        weight.equals(age) shouldBe false
        gender.equals(age) shouldBe false
      }
    }
    "hashcode" should {
      "be overridden to return the uid" in {
        age.hashCode shouldBe age.uid.hashCode
      }
    }
    "name" should {
      "be the same as val name" in {
        age.name shouldBe "age"
        gender.name shouldBe "gender"
        height.name shouldBe "height"
        weight.name shouldBe "weight"
        description.name shouldBe "description"
        boarded.name shouldBe "boarded"
        stringMap.name shouldBe "stringMap"
        numericMap.name shouldBe "numericMap"
        booleanMap.name shouldBe "booleanMap"
        survived.name shouldBe "survived"

        /* The features below are the outputs of estimators or transformers which will not actually be fit until fit method
       * is called somewhere in the pipeline
       */
        // simple interaction transformation syntax
        // Full transformer would look like:
        // val density = DivideTransformer.setNumerator(weight).setDenominator(height).getOutput
        //  val density = weight / height
        //
        //  // Full transformer would look like:
        //  // pivotedGender = PivotEstimator.setPivotSize(2).setInputCol(gender) // gender.pivot()
        //  val pivotedGender = new PivotEstimator.setPivotSize(2).setInputCol(gender)
        //
        //  val boardedDaysAgo = new DaysAgoTransformer.setInputCol(boarded)
        //
        //  // have all normalizations be part of one transformer
        //  val normedAge = new NormEstimator.setNormType(NormTypes.range).setInputCol(age)
        //
        //  // allow pivots into bins by correlation with label
        //  val stringMapPivot = new PivotEstimator.setPivotSize(3).setCorrelatedWith(survived).setInputCol(stringMap)
        //
        //  val descriptionHash = new HashTransformer.setNumberHashes(5).setInputCol(description)
        //
        //  val survivedNumeric = new NumericTransformer.setInputCol(survived)
      }
      "can be changed" in {
        val foo = age.alias
        foo.name shouldBe "foo"
        val bar = age.alias("bar")
        bar.name shouldBe "bar"
      }
    }
    "isSubtypeOf" should {
      "correctly figure out feature subtypes" in {
        survived.isSubtypeOf[SingleResponse] shouldBe true
        boardedTime.isSubtypeOf[SingleResponse] shouldBe false
      }
    }
    "traverse" should {
      "operate correctly" in {
        (height + weight * age).traverse(List.empty[String])((acc, f) =>
          if (acc.contains(f.uid)) acc else f.uid :: acc
        ).length shouldBe 5
      }
      "collect raw features" in {
        height.rawFeatures shouldBe Seq(height)
        (height + weight).rawFeatures should contain theSameElementsAs Seq(height, weight)
      }
      "collect all features" in {
        height.allFeatures shouldBe Seq(height)
        val plus = height + weight
        plus.allFeatures should contain theSameElementsAs Seq(plus, height, weight)
      }
      "collect parent stages with correct distances" in {
        val plus = height + weight
        val mul = plus * age
        val div = mul / height
        div.parentStages() match {
          case Failure(e) => fail(e)
          case Success(stages) =>
            stages.size shouldBe 3
            stages.get(plus.originStage) shouldBe Some(2)
            stages.get(mul.originStage) shouldBe Some(1)
            stages.get(div.originStage) shouldBe Some(0)
        }
      }

    }
    // TODO: test other feature methods

  }

}
