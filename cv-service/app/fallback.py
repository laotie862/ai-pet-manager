from PIL import Image, ImageStat


def classify_placeholder(image: Image.Image) -> tuple[str, float]:
    small = image.resize((32, 32))
    stat = ImageStat.Stat(small)
    r, g, b = stat.mean
    brightness = (r + g + b) / 3
    color_bias = max(r, g, b) - min(r, g, b)

    if brightness < 35:
        return "sleeping", 0.76
    if b > r + 18 and b > g + 8:
        return "drinking", 0.73
    if g > r + 12 and g > b + 6:
        return "exercising", 0.72
    if r > g + 12 and r > b + 6:
        return "eating", 0.74
    if color_bias < 10 and brightness < 120:
        return "defecating", 0.69
    return "uncertain", 0.45


def classify_fallback(image: Image.Image) -> tuple[bool, str, float]:
    if likely_sleeping_scene(image):
        return True, "sleeping", 0.64
    if likely_eating_scene(image):
        return True, "eating", 0.62
    return True, "exercising", 0.55


def likely_eating_scene(image: Image.Image) -> bool:
    small_width = 160
    scale = small_width / image.width
    small_height = max(1, int(image.height * scale))
    small = image.convert("RGB").resize((small_width, small_height))
    start_y = small_height // 4
    pixels = small.load()
    visited: set[tuple[int, int]] = set()

    def is_dark(x: int, y: int) -> bool:
        r, g, b = pixels[x, y]
        return (r + g + b) / 3 < 65

    def has_nearby_pet_body(min_x: int, min_y: int, max_x: int, max_y: int) -> bool:
        pad = 18
        x0 = max(0, min_x - pad)
        y0 = max(start_y, min_y - pad)
        x1 = min(small_width - 1, max_x + pad)
        y1 = min(small_height - 1, max_y + pad)
        bright = 0
        total = 0
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                r, g, b = pixels[x, y]
                total += 1
                if r > 170 and g > 170 and b > 160:
                    bright += 1
        return total > 0 and bright / total > 0.08

    for y in range(start_y, small_height):
        for x in range(small_width):
            if (x, y) in visited or not is_dark(x, y):
                continue
            area, min_x, min_y, max_x, max_y = flood_fill(x, y, visited, is_dark, small_width, small_height, start_y)
            width = max_x - min_x + 1
            height = max_y - min_y + 1
            ratio = width / max(1, height)
            center_y = (min_y + max_y) / 2
            in_feeding_area = center_y > small_height * 0.48
            compact_bowl_like = 35 <= area <= 900 and 0.5 <= ratio <= 2.0
            if in_feeding_area and compact_bowl_like and has_nearby_pet_body(min_x, min_y, max_x, max_y):
                return True
    return False


def likely_sleeping_scene(image: Image.Image) -> bool:
    small_width = 160
    scale = small_width / image.width
    small_height = max(1, int(image.height * scale))
    small = image.convert("RGB").resize((small_width, small_height))
    start_y = small_height // 3
    pixels = small.load()
    visited: set[tuple[int, int]] = set()

    def is_pet_like(x: int, y: int) -> bool:
        r, g, b = pixels[x, y]
        brightness = (r + g + b) / 3
        color_spread = max(r, g, b) - min(r, g, b)
        return brightness > 145 and color_spread < 45

    for y in range(start_y, small_height):
        for x in range(small_width):
            if (x, y) in visited or not is_pet_like(x, y):
                continue
            area, min_x, min_y, max_x, max_y = flood_fill(
                x, y, visited, is_pet_like, small_width, small_height, start_y
            )
            width = max_x - min_x + 1
            height = max_y - min_y + 1
            ratio = width / max(1, height)
            lower_half = min_y > small_height * 0.38
            horizontal_body = ratio >= 1.08 and width >= 20 and height >= 7
            large_enough = area >= 70
            if lower_half and horizontal_body and large_enough:
                return True
    return False


def flood_fill(
    x: int,
    y: int,
    visited: set[tuple[int, int]],
    predicate,
    width: int,
    height: int,
    min_y_bound: int,
) -> tuple[int, int, int, int, int]:
    stack = [(x, y)]
    visited.add((x, y))
    area = 0
    min_x = max_x = x
    min_y = max_y = y

    while stack:
        cx, cy = stack.pop()
        area += 1
        min_x = min(min_x, cx)
        max_x = max(max_x, cx)
        min_y = min(min_y, cy)
        max_y = max(max_y, cy)
        for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
            if 0 <= nx < width and min_y_bound <= ny < height and (nx, ny) not in visited and predicate(nx, ny):
                visited.add((nx, ny))
                stack.append((nx, ny))

    return area, min_x, min_y, max_x, max_y
