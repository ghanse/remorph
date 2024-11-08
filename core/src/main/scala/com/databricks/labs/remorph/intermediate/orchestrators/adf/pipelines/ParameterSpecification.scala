package com.databricks.labs.remorph.intermediate.orchestrators.adf.pipelines

import com.databricks.labs.remorph.intermediate.orchestrators.adf.PipelineNode

case class ParameterSpecification(name: Option[String], default: Option[String]) extends PipelineNode {
  override def children: Seq[PipelineNode] = Seq()
}
