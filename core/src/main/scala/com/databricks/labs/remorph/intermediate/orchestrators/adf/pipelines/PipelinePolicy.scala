package com.databricks.labs.remorph.intermediate.orchestrators.adf.pipelines

import com.databricks.labs.remorph.intermediate.orchestrators.adf.PipelineNode

case class PipelinePolicy(elapsedTimeMetric: ElapsedTimeMetric) extends PipelineNode {
  override def children: Seq[PipelineNode] = Seq()
}
