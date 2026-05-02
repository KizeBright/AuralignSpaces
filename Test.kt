package test
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode
fun main() {
    val methods = ARSceneView::class.java.methods.map { it.name }
    println(methods.filter { it.contains("Child") || it.contains("Node") })
}
