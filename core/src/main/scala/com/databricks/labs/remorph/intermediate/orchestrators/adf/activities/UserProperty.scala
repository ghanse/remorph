package com.databricks.labs.remorph.intermediate.orchestrators.adf.activities

import com.databricks.labs.remorph.intermediate.orchestrators.adf.PipelineNode

case class UserProperty(name: Option[String], value: Option[String]) extends PipelineNode {
  override def children: Seq[PipelineNode] = Seq()
}
