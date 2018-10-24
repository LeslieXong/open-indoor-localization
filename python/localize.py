import numpy as np
import pandas as pd
from sklearn.neighbors import NearestNeighbors


class Fingerprint:
    def __init__(self):
        self.use_type = None
        self.test = None
        self.train = None
        self.sorted_mac = None
        self.filter_mac = None

    def __process(self, file_name):
        file = open(file_name, "r")
        line = file.readline()
        data_dict = {'grid_id': [], 'x': [], 'y': [], 'mac': [], 'type': [], 'rssi': []}
        grid_id = 0
        while True:
            if line.strip() is not '':
                grid_id += 1
                grid_title = line.split(" ")
                grid_pos = [float(grid_title[0]), float(grid_title[1])]
                for i in range(int(grid_title[2])):
                    line = file.readline().rstrip("\n")
                    record = line.split("|")
                    ap = record[0].split(" ")
                    data_dict['grid_id'].append(grid_id)
                    data_dict['x'].append(grid_pos[0])
                    data_dict['y'].append(grid_pos[1])
                    data_dict['mac'].append(ap[0][0:17])
                    data_dict['type'].append(ap[2])
                    rssi_list = record[1].strip().split(' ') if record[1].strip() != '' else [ap[1]]
                    data_dict['rssi'].append(float(np.array(rssi_list).astype(np.float).mean()))
            line = file.readline()
            if not line:
                break
        file.close()

        data_frame = pd.DataFrame.from_dict(data_dict)
        # using grid_id for grouping to avoid group same positions's data together if use position for grouping
        grouped = data_frame.groupby(by='grid_id')

        grid_sum = len(grouped.groups)
        if not self.sorted_mac:
            mac = set(data_frame[data_frame['type'] == self.use_type]['mac'].unique())
            self.sorted_mac = sorted(mac & self.filter_mac if self.filter_mac else mac)
        sigma = np.full(shape=(grid_sum, len(self.sorted_mac)), fill_value=-100.)
        theta = np.full(shape=(grid_sum, len(self.sorted_mac)), fill_value=-2.)
        prior = np.full(grid_sum, 1. / grid_sum)

        grid_index = 0
        grid_position = np.full(shape=(grid_sum, 2), fill_value=0)
        for name, group in grouped:
            for index, row in group.iterrows():
                if row['mac'] in self.sorted_mac:
                    ap_index = self.sorted_mac.index(row['mac'])
                    theta[grid_index, ap_index] = row['rssi']
            grid_position[grid_index] = [group["x"].mean(), group['y'].mean()]
            grid_index += 1

        return {"prior": prior, "theta": theta, "sigma": sigma, "position": grid_position}

    def set_filter_mac(self, mac_list):
        self.filter_mac = set(mac_list) if mac_list else None

    def prepare_data(self, train_path, test_path, use='w'):
        self.use_type = use
        self.train = self.__process(train_path)
        self.test = self.__process(test_path)


def sklearn_knn(train_dict, test_dict, k=4):
    neigh = NearestNeighbors(n_neighbors=k)
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
    fingerprint = Fingerprint()
    # set filter list here.
    fingerprint.set_filter_mac(None)
    # prepare train and test data, default use wifi data, 'b' if want beacon used
    fingerprint.prepare_data('train.txt', 'test.txt', 'w')
    # knn localize and output result.
    result = sklearn_knn(fingerprint.train, fingerprint.test, k=4)
    rmse = sum(result[:, 2]) / float(len(result[:, 2]))
    print(" test points:{:5} , rmse:{:.5}".format(fingerprint.test["position"].shape[0], rmse))
