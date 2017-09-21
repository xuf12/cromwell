package cromwell.backend.impl.spark

import akka.actor.{ActorRef, Props}
import cromwell.backend._
import cromwell.backend.io.JobPathsWithDocker
import cromwell.backend.sfs.SharedFileSystemExpressionFunctions
import cromwell.core.CallContext
import wdl4s.wom.expression.IoFunctionSet
import wdl4s.wom.graph.TaskCallNode

case class SparkBackendFactory(name: String, configurationDescriptor: BackendConfigurationDescriptor) extends BackendLifecycleActorFactory {
  override def workflowInitializationActorProps(workflowDescriptor: BackendWorkflowDescriptor, ioActor: ActorRef,
                                                calls: Set[TaskCallNode], serviceRegistryActor: ActorRef, restarting: Boolean): Option[Props] = {
    Option(SparkInitializationActor.props(workflowDescriptor, calls, configurationDescriptor, serviceRegistryActor))
  }

  override def jobExecutionActorProps(jobDescriptor: BackendJobDescriptor,
                                      initializationData: Option[BackendInitializationData],
                                      serviceRegistryActor: ActorRef,
                                      ioActor: ActorRef,
                                      backendSingletonActor: Option[ActorRef]): Props = {
    SparkJobExecutionActor.props(jobDescriptor, configurationDescriptor)
  }

  override def expressionLanguageFunctions(workflowDescriptor: BackendWorkflowDescriptor, jobKey: BackendJobDescriptorKey,
                                           initializationData: Option[BackendInitializationData]): IoFunctionSet = {
    val jobPaths = JobPathsWithDocker(jobKey, workflowDescriptor, configurationDescriptor.backendConfig)
    val callContext = CallContext(
      jobPaths.callExecutionRoot,
      jobPaths.stdout.toAbsolutePath.toString,
      jobPaths.stderr.toAbsolutePath.toString
    )

    new SharedFileSystemExpressionFunctions(SparkJobExecutionActor.DefaultPathBuilders, callContext)
  }
}
