package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.List;
import java.util.Random;

/**
 * Fruchterman–Reingold spring embedder, followed by a few overlap-removal passes.
 *
 * <p>Deliberately the <em>second</em> option, not the default: it has no idea which way data flows,
 * so a pipeline laid out this way reads as a blob. What it is good at is the case
 * {@link LayeredLayout} handles worst — a densely cyclic graph (a state machine, a feedback network)
 * where there is no meaningful left-to-right order to find, and clustering by connectivity is the
 * only structure worth showing.</p>
 *
 * <p>Seeding is from a circle rather than from the current positions, on purpose: starting from a
 * mess converges to a differently-shaped mess, and two boxes sitting exactly on top of each other
 * would have no direction to separate along.</p>
 */
public class ForceDirectedLayout implements IGraphLayout {

    @Override
    public void layout(List<LayoutBox> boxes, List<LayoutEdge> edges, LayoutOptions options) {
        var n = boxes.size();
        if (n == 0) return;
        if (n == 1) {
            boxes.get(0).x = 0;
            boxes.get(0).y = 0;
            return;
        }

        var spacing = options.nodeSpacing();
        var averageWidth = 0f;
        var averageHeight = 0f;
        for (var box : boxes) {
            averageWidth += box.width;
            averageHeight += box.height;
        }
        averageWidth /= n;
        averageHeight /= n;

        // Ideal edge length: the geometric mean of a "cell" big enough to hold an average box plus
        // its spacing — the usual FR k = sqrt(area / n), with the area estimated from the content.
        var k = (float) Math.sqrt((averageWidth + spacing) * (averageHeight + spacing));
        var linkLength = k + options.layerSpacing() * 0.25f;

        var centreX = new float[n];
        var centreY = new float[n];
        var random = new Random(options.seed());
        var radius = linkLength * (float) Math.sqrt(n);
        for (var i = 0; i < n; i++) {
            var angle = (float) (2 * Math.PI * i / n);
            // A touch of jitter breaks the perfect symmetry a ring start would otherwise hold on to.
            centreX[i] = (float) (Math.cos(angle) * radius + (random.nextFloat() - 0.5f) * linkLength);
            centreY[i] = (float) (Math.sin(angle) * radius + (random.nextFloat() - 0.5f) * linkLength);
        }

        var iterations = Math.max(60, options.iterations() * 40);
        var temperature = radius / 3f;
        var cooling = temperature / (iterations + 1);
        var dx = new float[n];
        var dy = new float[n];

        for (var step = 0; step < iterations; step++) {
            java.util.Arrays.fill(dx, 0f);
            java.util.Arrays.fill(dy, 0f);

            for (var i = 0; i < n; i++) {
                for (var j = i + 1; j < n; j++) {
                    var ox = centreX[i] - centreX[j];
                    var oy = centreY[i] - centreY[j];
                    var distance = (float) Math.sqrt(ox * ox + oy * oy);
                    if (distance < 1e-3f) {
                        ox = (random.nextFloat() - 0.5f);
                        oy = (random.nextFloat() - 0.5f);
                        distance = 1e-3f;
                    }
                    var force = linkLength * linkLength / distance;
                    var fx = ox / distance * force;
                    var fy = oy / distance * force;
                    dx[i] += fx;
                    dy[i] += fy;
                    dx[j] -= fx;
                    dy[j] -= fy;
                }
            }

            for (var edge : edges) {
                if (edge.from() == edge.to()) continue;
                if (edge.from() < 0 || edge.from() >= n || edge.to() < 0 || edge.to() >= n) continue;
                var ox = centreX[edge.from()] - centreX[edge.to()];
                var oy = centreY[edge.from()] - centreY[edge.to()];
                var distance = (float) Math.sqrt(ox * ox + oy * oy);
                if (distance < 1e-3f) continue;
                var force = distance * distance / linkLength;
                var fx = ox / distance * force;
                var fy = oy / distance * force;
                dx[edge.from()] -= fx;
                dy[edge.from()] -= fy;
                dx[edge.to()] += fx;
                dy[edge.to()] += fy;
            }

            for (var i = 0; i < n; i++) {
                var length = (float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
                if (length < 1e-4f) continue;
                var limited = Math.min(length, temperature);
                centreX[i] += dx[i] / length * limited;
                centreY[i] += dy[i] / length * limited;
            }
            temperature = Math.max(0f, temperature - cooling);
        }

        for (var i = 0; i < n; i++) {
            var box = boxes.get(i);
            box.x = centreX[i] - box.width / 2f;
            box.y = centreY[i] - box.height / 2f;
        }
        separate(boxes, spacing);
    }

    /**
     * Pushes overlapping boxes apart along whichever axis they overlap least on. The simulation
     * treats every node as a point, so two large boxes can settle at a comfortable centre distance
     * and still visibly overlap; this is what makes the result usable rather than correct.
     */
    static void separate(List<LayoutBox> boxes, float spacing) {
        var n = boxes.size();
        for (var pass = 0; pass < 24; pass++) {
            var moved = false;
            for (var i = 0; i < n; i++) {
                for (var j = i + 1; j < n; j++) {
                    var a = boxes.get(i);
                    var b = boxes.get(j);
                    var overlapX = (a.width + b.width) / 2f + spacing - Math.abs((a.x + a.width / 2f) - (b.x + b.width / 2f));
                    var overlapY = (a.height + b.height) / 2f + spacing - Math.abs((a.y + a.height / 2f) - (b.y + b.height / 2f));
                    if (overlapX <= 0 || overlapY <= 0) continue;
                    moved = true;
                    if (overlapX < overlapY) {
                        var push = overlapX / 2f;
                        var sign = (a.x + a.width / 2f) <= (b.x + b.width / 2f) ? -1f : 1f;
                        a.x += sign * push;
                        b.x -= sign * push;
                    } else {
                        var push = overlapY / 2f;
                        var sign = (a.y + a.height / 2f) <= (b.y + b.height / 2f) ? -1f : 1f;
                        a.y += sign * push;
                        b.y -= sign * push;
                    }
                }
            }
            if (!moved) return;
        }
    }
}
