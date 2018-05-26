if __name__ == "__main__":
    info_mapa = {}
    with open('output_info.txt', 'r') as input:
        for line in input:
            if eval(line)['metre'] in info_mapa:
                info_mapa[eval(line)['metre']].append(eval(line)['angle'])
            else:
                info_mapa[eval(line)['metre']] = [eval(line)['angle']]
    from numpy import mean
    info_mapa = dict(map(lambda item: (item[0], mean(item[1])), info_mapa.iteritems()))

    # Rectifiquem els primers 100 metres del circuit

    for metre, angle in info_mapa.items()[:150]+info_mapa.items()[-20:]:
        info_mapa[metre] *= 0.05

    RANG_METRES = 50
    for i in range(0, len(info_mapa)-RANG_METRES):
        info_mapa[i] = mean([info_mapa[j] for j in range(i, i + RANG_METRES)])
    for i in range(len(info_mapa)-RANG_METRES, len(info_mapa)):
        info_mapa[i] = mean(
            [info_mapa[j] for j in range(i, len(info_mapa))] +
            [info_mapa[j] for j in range(0, i-len(info_mapa)+RANG_METRES)]
        )

    # Normalitzem els angles
    max_angle = max(max(info_mapa.values()), abs(min(info_mapa.values())))
    info_mapa = dict(map(lambda item: (item[0], item[1] / max_angle), info_mapa.iteritems()))

    with open('info_mapa.txt', 'w+') as output:
        for metre, angle in info_mapa.items():
            output.write('{} {}\n'.format(metre, angle))
