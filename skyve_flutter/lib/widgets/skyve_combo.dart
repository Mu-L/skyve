import 'package:flutter/material.dart';
import 'package:skyve_flutter/util/skyve_mixins.dart';

class SkyveCombo extends StatelessWidget with Sizable {
  final String label;

  SkyveCombo({super.key, required this.label, int? pixelWidth}) {
    this.pixelWidth = pixelWidth;
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement widget
    Widget result = Stack(alignment: Alignment.center, children: [
      const Placeholder(fallbackHeight: 50, color: Colors.orange),
      Text('Combo: $label')
    ]);
    if (pixelWidth != null) {
      result = SizedBox(width: pixelWidth!.toDouble(), child: result);
    }
    return result;
  }
}
