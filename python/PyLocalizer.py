import numpy as np
from sklearn.neighbors import NearestNeighbors


def count_sorted_mac(database_file_name):
    file = open(database_file_name, "r")
    beacon_mac_set = set()
    wifi_mac_set = set()

    line = file.readline()
    while True:
        if line.strip() is not '':
            attr = line.split(" ")
            ap_num = int(attr[2])
            for i in range(ap_num):
                line = file.readline().rstrip("\n")
                attr = line.split("|")[0].split(" ")
                if attr[2] == 'b':
                    beacon_mac_set.add(attr[0])
                else:
                    wifi_mac_set.add(attr[0])

        line = file.readline()
        if not line:
            break

    file.close()
    return sorted(beacon_mac_set), sorted(wifi_mac_set)


def read_data(file_name, sorted_mac=None):
    '''
    :param file_name: the input file name
    :param sorted_mac: sorted_mac :used for rssi sort index(dimensional)
    :return: m*n theta(mean) and sigma(std) result , m represent point number and n represent ap(feature) number.
    '''

    file = open(file_name, "r")

    ap_info = []
    position = []
    point_index = -1

    line = file.readline()
    while True:
        if line.strip() is not '':
            attr = line.split(" ")
            point_pos = [float(attr[0]), float(attr[1])]
            ap_num = int(attr[2])
            position.append(point_pos)
            point_index += 1

            for i in range(ap_num):
                line = file.readline().rstrip("\n")
                record = line.split("|")
                attr = record[0].split(" ")
                if not sorted_mac or attr[0][0:17] in sorted_mac:
                    rssi_list = record[1].strip().split(" ") if record[1].strip() != "" else None
                    ap_info.append([point_index, attr, rssi_list])

        line = file.readline()
        if not line:
            break

    file.close()

    point_sum = point_index + 1
    sigma = np.full(shape=(point_sum, len(sorted_mac)), fill_value=-100.)
    theta = np.full(shape=(point_sum, len(sorted_mac)), fill_value=-2.)
    prior = np.full(point_sum, 1. / point_sum)

    for xi in ap_info:
        point_index = xi[0]
        ap_index = sorted_mac.index(xi[1][0][0:17])
        if ap_index is not None:  # this ap existed
            rssi_list = np.array(xi[2]).astype(np.float)
            theta[point_index, ap_index] = np.mean(rssi_list)
            # sigma[point_index, ap_index] = float(xi[1][2])

    position = np.array(position)
    return {"prior": prior, "theta": theta, "sigma": sigma, "position": position}


def cal_distance(row1, row2, metric='Coef'):
    if metric is 'Coef':
        coef = np.corrcoef(row1, row2)[0, 1]
        distance = 1. - coef if coef > 0 else 1.
    elif metric is 'Euclid':
        distance = np.linalg.norm(row1 - row2)
    elif metric is 'Weight_euclid':
        distance = np.linalg.norm((row1 - row2) * (1 / row1))
    elif metric is 'Chebyshev':
        distance = np.max(np.abs(row1 - row2))
    elif metric is 'Manhattan':
        distance = np.sum(np.abs(row1 - row2))
    else:
        distance = np.linalg.norm((row1, row2))
    return distance


def sklearn_knn(train_dict, test_dict, k=4):
    neigh = NearestNeighbors(n_neighbors=k, metric=cal_distance)
    neigh.fit(train_dict['theta'])
    predict_neighbor = neigh.kneighbors(test_dict['theta'])

    predict_position = []
    for i in range(len(test_dict['theta'])):
        k_distances = predict_neighbor[0][i]
        k_indexs = predict_neighbor[1][i]

        predict_position.append(
            np.average(train_dict['position'][k_indexs], axis=0))

    error = np.linalg.norm(np.array(predict_position) - test_dict['position'], axis=1)
    error = np.transpose([error])
    return np.append(np.array(predict_position), error, axis=1)


if __name__ == '__main__':
    beacon_set, wifi_set = count_sorted_mac('train.txt')

    use_which = wifi_set  # or beacon_set if use wifi for positioning

    train_data = read_data('train.txt', use_which)
    test_data = read_data('test.txt', use_which)

    error = sklearn_knn(train_data, test_data, k=4)
    rmse = sum(error[:, 2]) / float(len(error[:, 2]))
    print(" test points:{:5} , rmse:{:.5}".format(test_data["position"].shape[0], rmse))
