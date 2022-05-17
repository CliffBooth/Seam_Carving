package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

fun main(args: Array<String>) {
    var input = "in.png"
    var output = "out.png"
    var width = 100
    var height = 100

    val helpMessage = """Welcome to seam carving program!
        usage: 
        -in <inputImage.png>
        -out <outputImage.png>
        -width <by how much you want to reduce your imag in width (integer)>
        -height <by how much you want to reduce your image in height (integer)>
        -h | -help | --help - to see this message
    """.trimMargin()

    if (args.isEmpty()) {
        println(helpMessage)
        return
    }

    for (i in args.indices) {
        when (args[i]) {
            "-in" -> input = args[i + 1]
            "-out" -> output = args[i + 1]
            "-width" -> width = args[i + 1].toInt()
            "-height" -> height = args[i + 1].toInt()
            "-h", "-help", "--help" -> {
                println(helpMessage)
                return
            }
        }
    }
    println("Arguments are read!")
    var image = ImageIO.read(File(input))
    if (image.width <= width || image.height <= height)
        throw IllegalArgumentException(
            "image is smaller than the number of pixels you are trying to remove!\n" +
                    "image width = ${image.width}; -width = $width\n" +
                    "image height = ${image.height}; -height = $height"
        )

//    removing vertical seams
    var count = 1
    repeat(width) {
        image = removeVerticalSeam(image)
        if (count % 10 == 0)
            println("vertical seams processed: $count")
        count++
    }
    //removing horizontal seams
    count = 1
    image = transposeImage(image)
    repeat(height) {
        image = removeVerticalSeam(image)
        if (count % 10 == 0)
            println("horizontal seams processed: $count")
        count++
    }
    image = transposeImage(image)
    ImageIO.write(image, "png", File(output))

}

class Node(val x: Int, val y: Int, val energy: Double, var distance: Double, var previous: Node?) {
    constructor() : this(0, 0, 0.0, 0.0, null)
}

fun removeVerticalSeam(image: BufferedImage): BufferedImage {
    val result = BufferedImage(image.width - 1, image.height, image.type)
    val seam = findSeam(image)
    for (i in 0 until result.width) {
        for (j in 0 until result.height) {
            val rgb = if (i < seam[j].x) image.getRGB(i, j) else image.getRGB(i + 1, j)
            result.setRGB(i, j, rgb)
        }
    }
    return result
}

//returns the list of nodes, each corresponds to a pixel within a seam on the picture, index of each node corresponds to y position.
fun findSeam(image: BufferedImage): List<Node> {
    //add additional row in the beginning and in the end
    //2d array of nodes == the graph
    val graph = Array(image.width) { Array(image.height + 2) { Node() } }
    for (i in 0 until image.width) {
        for (j in 0..image.height + 1) {
            val energy = if (j == 0 || j == image.height + 1) 0.0 else energy(image, i, j - 1)
            val distance = if (i == 0 && j == 0) 0.0 else Double.MAX_VALUE
            graph[i][j] = Node(i, j, energy, distance, null)
        }
    }

    //implementing Dijkstra's algorithm
    val queue = PriorityQueue<Node> { o1, o2 -> o1.distance.compareTo(o2.distance) }
    val start = graph[0][0]
    queue.add(start)
    val target = graph[image.width - 1][image.height + 1] //the target node
    val processed = mutableSetOf<Node>()

    while (target !in processed) {
        val current = queue.poll()
        if (current in processed)
            continue
        val neighbors = mutableListOf<Node>()
        //neighbors of the node are only those which are under the node!
        for (x in -1..1) {
            try {
                neighbors.add(graph[current.x + x][current.y + 1])
            } catch (e: IndexOutOfBoundsException) {
            }
        }
        //also if the node is in the first or the last row, its neighbor is the one on the right
        if (current.y == 0 || current.y == image.height + 1) {
            try {
                neighbors.add(graph[current.x + 1][current.y])
            } catch (e: IndexOutOfBoundsException) {
            }
        }

        for (n in neighbors) {
            if (n in processed)
                continue
            val newDist = current.distance + n.energy
            if (newDist < n.distance) {
                n.distance = newDist
                n.previous = current
            }
            queue.add(n)
        }

        processed.add(current)
    }

    val result = mutableListOf<Node>()
    var node = target

    //add all the nodes that correspond to a pixel on the picture to the list.
    while (node != start) {
        node = node.previous ?: throw Exception("couldn't build a seam =(")
        if (node.y != 0 && node.y != image.height + 1)
            result += node
    }
    return result.reversed()
}

fun transposeImage(image: BufferedImage): BufferedImage {
    val result = BufferedImage(image.height, image.width, image.type)
    for (i in 0 until image.width) {
        for (j in 0 until image.height) {
            result.setRGB(j, i, image.getRGB(i, j))
        }
    }
    return result
}

fun energy(image: BufferedImage, x: Int, y: Int): Double {
    var newX: Int = x
    var newY: Int = y

    if (x == 0)
        newX = 1
    if (x == image.width - 1)
        newX = image.width - 2
    if (y == 0)
        newY = 1
    if (y == image.height - 1)
        newY = image.height - 2

    val cx1 = Color(image.getRGB(newX + 1, y))
    val cx2 = Color(image.getRGB(newX - 1, y))
    val cy1 = Color(image.getRGB(x, newY + 1))
    val cy2 = Color(image.getRGB(x, newY - 1))
    val dx = (cx1.red - cx2.red).toDouble().pow(2.0) +
            (cx1.green - cx2.green).toDouble().pow(2.0) +
            (cx1.blue - cx2.blue).toDouble().pow(2.0)
    val dy = (cy1.red - cy2.red).toDouble().pow(2.0) +
            (cy1.green - cy2.green).toDouble().pow(2.0) +
            (cy1.blue - cy2.blue).toDouble().pow(2.0)
    return sqrt(dy + dx)
}