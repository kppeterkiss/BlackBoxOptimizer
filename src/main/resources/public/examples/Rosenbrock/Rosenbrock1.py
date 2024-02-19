
def rosen(x):
     #"""The Rosenbrock function"""
     return sum(100.0*(x[1:]-x[:-1]**2.0)**2.0 + (1-x[:-1])**2.0)

print(sys.argv)
x = np.array([float(i) for i in sys.argv[1:]]) #[float(i) for i in sys.argv[3:]]

print('rosen '+ str(rosen(x))+"\n")
print('rosen1 '+ str(rosen(x)+1))
